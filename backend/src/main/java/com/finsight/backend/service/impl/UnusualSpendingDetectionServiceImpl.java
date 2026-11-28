package com.finsight.backend.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.client.AiAnomalyClient;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.UnusualSpendingDetectionService;

import lombok.RequiredArgsConstructor;

/**
 * Unusual spending detection for one upload. Loads the user's transactions for that
 * upload and asks the Python AI service to flag unusually large expenses. Read-only.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnusualSpendingDetectionServiceImpl implements UnusualSpendingDetectionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final AiAnomalyClient aiAnomalyClient;

    /**
     * Ownership check (upload must belong to the user's profile), AI call, then the
     * upload id and file name are added to the response so the UI can show where the
     * result came from.
     */
    @Override
    public AnomalyDetectionResponse detectAnomalies(Long uploadHistoryId) {
        Objects.requireNonNull(uploadHistoryId, "Upload history ID must not be null.");

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);
        UploadHistory uploadHistory = uploadHistoryRepository
                .findByIdAndFinancialProfile(uploadHistoryId, profile)
                .orElseThrow(() -> new IllegalArgumentException("Upload history not found."));

        List<Transaction> transactions = transactionRepository
                .findByUploadHistoryAndUserOrderByTransactionDateDesc(uploadHistory, user);
        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions were found for this upload.");
        }

        AnomalyDetectionResponse response = aiAnomalyClient.detect(transactions);
        response.setUploadHistoryId(uploadHistory.getId());
        response.setFileName(uploadHistory.getOriginalFileName());
        return response;
    }

    /**
     * Resolve the logged-in user from the Spring SecurityContext (the JWT filter
     * stores the email there) and load the User. Same helper is repeated in most
     * service classes.
     */
    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user was found.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    /**
     * The user's profile; a missing profile is an error because uploads are stored
     * against it.
     */
    private FinancialProfile getFinancialProfile(User user) {
        return financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Financial profile was not found."));
    }
}
