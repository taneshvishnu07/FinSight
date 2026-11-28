package com.finsight.backend.service.impl;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.client.AiSubscriptionClient;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.User;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.SubscriptionDetectionService;

import lombok.RequiredArgsConstructor;

/**
 * Subscription detection. Unlike the upload-based services it analyses ALL of the
 * logged-in user's transactions so recurring payments are visible across several
 * uploaded files.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubscriptionDetectionServiceImpl implements SubscriptionDetectionService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final AiSubscriptionClient aiSubscriptionClient;

    /**
     * Load every transaction of the user and let the AI service find recurring monthly
     * subscriptions.
     */
    @Override
    public SubscriptionDetectionResponse detectSubscriptions() {
        User user = getAuthenticatedUser();
        List<Transaction> transactions = transactionRepository.findByUserOrderByTransactionDateDesc(user);
        if (transactions == null) {
            transactions = List.of();
        }
        return aiSubscriptionClient.detect(transactions);
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
