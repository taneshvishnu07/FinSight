/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.subscription;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class SubscriptionDetectionResponse {

    private int subscriptionCount;

    private BigDecimal estimatedMonthlySubscriptionCost;

    private BigDecimal estimatedYearlySubscriptionCost;

    private List<DetectedSubscription> subscriptions;


    @Getter
    @Setter
    @NoArgsConstructor
    public static class DetectedSubscription {

        private String merchant;

        private BigDecimal averageAmount;

        private String frequency;

        private int occurrences;

        private LocalDate firstTransactionDate;

        private LocalDate lastTransactionDate;

        private BigDecimal estimatedMonthlyCost;

        private BigDecimal estimatedYearlyCost;

        private double confidence;

        private List<LocalDate> transactionDates;
    }
}