package com.finsight.backend.service.impl;

import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.client.AiForecastClient;
import com.finsight.backend.dto.forecast.ForecastResponse;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.ForecastService;

import lombok.RequiredArgsConstructor;

/**
 * Expense forecasting: loads the upload's transactions (checking they belong to the
 * logged-in user) and delegates the prediction to the Python AI service. Read-only -
 * nothing is saved.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ForecastServiceImpl implements ForecastService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final AiForecastClient aiForecastClient;

    /**
     * Flow: current user -> financial profile -> upload (must belong to that profile)
     * -> its transactions -> AI forecast for `monthsAhead` months (minimum 1). Throws
     * IllegalArgumentException if the upload is not found or has no transactions.
     */
    @Override
    public ForecastResponse forecast(Long uploadHistoryId, int monthsAhead) {
        Objects.requireNonNull(uploadHistoryId, "Upload history ID must not be null.");

        User user = getAuthenticatedUser();
        FinancialProfile profile = financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("Financial profile was not found."));

        UploadHistory uploadHistory = uploadHistoryRepository
                .findByIdAndFinancialProfile(uploadHistoryId, profile)
                .orElseThrow(() -> new IllegalArgumentException("Upload history not found."));

        List<Transaction> transactions = transactionRepository
                .findByUploadHistoryAndUserOrderByTransactionDateDesc(uploadHistory, user);

        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException("No transactions were found for this upload.");
        }

        return aiForecastClient.forecast(transactions, Math.max(monthsAhead, 1));
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
}
