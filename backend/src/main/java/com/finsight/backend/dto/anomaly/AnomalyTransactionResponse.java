/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.anomaly;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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