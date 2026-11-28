package com.finsight.backend.dto.behaviour;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Spending behaviour result from the Python /api/behaviour-analysis endpoint: totals,
 * category breakdown and spending clusters.
 */
@Getter
@Setter
@NoArgsConstructor
public class BehaviourAnalysisResponse {

    private String status = "SUCCESS";
    private String message = "";
    private int transactionCount;
    private int expenseCount;
    private BigDecimal totalExpense = BigDecimal.ZERO;
    private BigDecimal averageExpense = BigDecimal.ZERO;
    private BigDecimal largestExpense = BigDecimal.ZERO;
    private String largestExpenseDescription = "";
    private List<CategoryBreakdown> categoryBreakdown = new ArrayList<>();
    private List<SpendingCluster> clusters = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class CategoryBreakdown {
        private String category = "";
        private BigDecimal amount = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SpendingCluster {
        private int cluster;
        private String label = "";
        private int transactionCount;
        private BigDecimal totalAmount = BigDecimal.ZERO;
        private BigDecimal averageAmount = BigDecimal.ZERO;
        private BigDecimal percentage = BigDecimal.ZERO;
    }
}
