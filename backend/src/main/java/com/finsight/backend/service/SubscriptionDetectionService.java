package com.finsight.backend.service;

import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;

/**
 * Detects recurring subscriptions through the AI service. Implementation:
 * SubscriptionDetectionServiceImpl.
 */
public interface SubscriptionDetectionService {

    /**
     * Analyse all of the logged-in user's transactions.
     */
    SubscriptionDetectionResponse detectSubscriptions();

}