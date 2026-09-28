/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnusualSpendingDetectionServiceImpl implements UnusualSpendingDetectionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final AiAnomalyClient aiAnomalyClient;

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

    private User getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user was found.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Authenticated user was not found."));
    }

    private FinancialProfile getFinancialProfile(User user) {
        return financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Financial profile was not found."));
    }
}
