package com.finsight.backend.dto.insight;

import java.math.BigDecimal;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Financial health score (0-100), status, savings rate and lists of insights, positive
 * behaviours, risks and priority actions.
 */
@Getter
@Setter
@NoArgsConstructor
public class FinancialInsightResponse {

    private int financialHealthScore;

    private String financialHealthStatus;

    private BigDecimal savingsRate;

    private String overallSummary;

    private List<Insight> insights;

    private List<String> positiveBehaviours;

    private List<String> financialRisks;

    private List<String> priorityActions;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class Insight {

        private String type;

        private String priority;

        private String title;

        private String message;

        private BigDecimal relatedAmount;

        private String relatedCategory;

        private String relatedMerchant;
    }
}