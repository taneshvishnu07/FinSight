/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.recommendation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RecommendationResponse {

    private Long uploadHistoryId;

    private String fileName;

    private LocalDate analysedFrom;

    private LocalDate analysedTo;

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal totalSavings;

    private BigDecimal estimatedMonthlySubscriptionCost;

    private BigDecimal estimatedYearlySubscriptionCost;

    private int unusualTransactionCount;

    private int recommendationCount;

    private List<RecommendationItemResponse> recommendations;

    private String summary;
}