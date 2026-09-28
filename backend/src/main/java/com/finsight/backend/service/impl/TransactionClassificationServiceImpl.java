/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.client.AiClassificationClient;
import com.finsight.backend.dto.classification.ClassificationItemResponse;
import com.finsight.backend.dto.classification.ClassificationResponse;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.TransactionCategory;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.TransactionClassificationService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionClassificationServiceImpl
        implements TransactionClassificationService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final AiClassificationClient aiClassificationClient;

    @Override
    public ClassificationResponse classifyUpload(Long uploadHistoryId) {
        if (uploadHistoryId == null) {
            throw new IllegalArgumentException("Upload history ID must not be null.");
        }

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);

        UploadHistory uploadHistory = uploadHistoryRepository
                .findByIdAndFinancialProfile(uploadHistoryId, profile)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Upload history not found."
                ));

        List<Transaction> transactions = transactionRepository
                .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                        uploadHistory,
                        user
                );

        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No transactions were found for this upload."
            );
        }

        ClassificationResponse aiResponse =
                aiClassificationClient.classify(transactions);

        List<ClassificationItemResponse> classifications =
                aiResponse.getClassifications();

        if (classifications == null) {
            classifications = new ArrayList<>();
        }

        for (ClassificationItemResponse classification : classifications) {
            if (classification == null || classification.getId() == null) {
                continue;
            }

            Transaction transaction = findTransactionById(
                    transactions,
                    classification.getId()
            );

            if (transaction == null) {
                classification.setUpdated(false);
                continue;
            }

            TransactionCategory mappedCategory = parseCategory(
                    classification.getMappedCategory()
            );

            if (mappedCategory == null) {
                classification.setUpdated(false);
                continue;
            }

            // Category is nullable in Transaction. Compare enum references directly
            // instead of dereferencing existingCategory, which keeps this null-safe.
            Transaction nonNullTransaction = Objects.requireNonNull(
                    transaction,
                    "Transaction must not be null."
            );

            TransactionCategory existingCategory = nonNullTransaction.getCategory();
            boolean updated = existingCategory == null
                    || existingCategory != mappedCategory;

            if (updated) {
                nonNullTransaction.setCategory(mappedCategory);
                transactionRepository.save(nonNullTransaction);
            }

            classification.setUpdated(updated);
        }

        aiResponse.setStatus("SUCCESS");
        aiResponse.setMessage(
                "Transaction classification completed and categories were updated."
        );
        aiResponse.setTotalTransactions(transactions.size());
        aiResponse.setClassifiedTransactions(classifications.size());
        aiResponse.setClassifications(classifications);

        return aiResponse;
    }

    private Transaction findTransactionById(
            List<Transaction> transactions,
            Long transactionId) {

        for (Transaction transaction : transactions) {
            if (transaction == null || transaction.getId() == null) {
                continue;
            }

            if (transaction.getId().equals(transactionId)) {
                return transaction;
            }
        }

        return null;
    }

    private TransactionCategory parseCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return TransactionCategory.valueOf(
                    value.trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private User getAuthenticatedUser() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("User is not authenticated.");
        }

        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated user not found."
                ));
    }

    private FinancialProfile getFinancialProfile(User user) {
        return financialProfileRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException(
                        "Financial profile not found."
                ));
    }
}
