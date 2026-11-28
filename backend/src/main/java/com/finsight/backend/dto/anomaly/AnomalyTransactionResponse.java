package com.finsight.backend.dto.anomaly;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One flagged expense: merchant, amount, date, anomaly score, severity
 * (MEDIUM/HIGH/CRITICAL) and an explanation.
 */
@Getter
@Setter
@NoArgsConstructor
public class AnomalyTransactionResponse {

    private Long transactionId;

    private String merchant;

    private BigDecimal amount;

    private String category;

    private LocalDate transactionDate;

    private BigDecimal anomalyScore;

    private String severity;

    private String reason;
}