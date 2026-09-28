/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.transaction;

import com.finsight.backend.enums.PaymentMethod;
import com.finsight.backend.enums.TransactionCategory;
import com.finsight.backend.enums.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {

    private Long id;

    private LocalDateTime transactionDate;

    private String description;

    private BigDecimal amount;

    private TransactionType type;

    private TransactionCategory category;

    private PaymentMethod paymentMethod;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}

