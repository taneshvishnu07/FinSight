package com.finsight.backend.dto.analysis;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import com.finsight.backend.enums.AnalysisStatus;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Result of the basic per-upload analysis: totals, counts, average/largest expense,
 * spending per category, subscription / unusual / recommendation counts and a summary
 * text.
 */
@Getter
@Setter
@NoArgsConstructor
public class TransactionAnalysisResponse {

    private Long analysisId;

    private Long analysisJobId;

    private Long uploadHistoryId;

    private String fileName;

    private AnalysisStatus status;

    private LocalDate analysedFrom;

    private LocalDate analysedTo;

    private Integer totalTransactions;

    private Integer incomeTransactions;

    private Integer expenseTransactions;

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal totalSavings;

    private BigDecimal averageExpense;

    private BigDecimal largestExpense;

    private String largestExpenseDescription;

    private Integer recurringSubscriptions;

    private Integer unusualTransactions;

    private Integer recommendationCount;

    private Map<String, BigDecimal> spendingByCategory;

    private String summary;

}