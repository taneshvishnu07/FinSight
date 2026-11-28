package com.finsight.backend.controller;

import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.service.SubscriptionDetectionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recurring subscription detection, mounted under /api/subscriptions. Works on all of
 * the logged-in user's transactions (not a single upload).
 */
@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
public class SubscriptionDetectionController {

    private final SubscriptionDetectionService
            subscriptionDetectionService;


    /*
     * ============================================================
     * DETECT SUBSCRIPTIONS
     * ============================================================
     *
     * GET /api/subscriptions/detect
     *
     */

    @GetMapping("/detect")
    public ResponseEntity<SubscriptionDetectionResponse>
            detectSubscriptions() {

        SubscriptionDetectionResponse response =
                subscriptionDetectionService
                        .detectSubscriptions();

        return ResponseEntity.ok(response);
    }
}