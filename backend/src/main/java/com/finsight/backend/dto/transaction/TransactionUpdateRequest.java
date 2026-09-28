/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.transaction;

import com.finsight.backend.enums.PaymentMethod;
import com.finsight.backend.enums.TransactionCategory;
import com.finsight.backend.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class TransactionUpdateRequest {

    private LocalDate transactionDate;

    private String description;

    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    private TransactionType type;

    private TransactionCategory category;

    private PaymentMethod paymentMethod;
}