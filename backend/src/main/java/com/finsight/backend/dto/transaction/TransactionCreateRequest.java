/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.finsight.backend.enums.PaymentMethod;
import com.finsight.backend.enums.TransactionCategory;
import com.finsight.backend.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransactionCreateRequest {

    @NotNull
    private LocalDate transactionDate;

    @NotBlank
    private String description;

    @NotNull
    @DecimalMin(value = "0.01")
    private BigDecimal amount;

    @NotNull
    private TransactionType type;

    private TransactionCategory category;

    private PaymentMethod paymentMethod;

    /*
     * Optional.
     *
     * Required only when the transaction came
     * from an uploaded file.
     */
    private Long uploadHistoryId;
}