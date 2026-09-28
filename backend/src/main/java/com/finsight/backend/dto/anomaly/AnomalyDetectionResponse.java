/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.anomaly;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AnomalyDetectionResponse {

    private Long analysisId;

    private Long uploadHistoryId;

    private String fileName;

    private LocalDate analysedFrom;

    private LocalDate analysedTo;

    private int totalTransactions;

    private int expenseTransactions;

    private BigDecimal averageExpense;

    private BigDecimal standardDeviation;

    private int anomalyCount;

    private List<AnomalyTransactionResponse> anomalies;

    private String summary;
}