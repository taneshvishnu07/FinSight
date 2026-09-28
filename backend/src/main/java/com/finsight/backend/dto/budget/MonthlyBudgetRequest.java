/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.budget;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class MonthlyBudgetRequest {

    @NotBlank
    @Pattern(regexp = "\\d{4}-(0[1-9]|1[0-2])")
    private String budgetMonth;

    @NotNull
    @DecimalMin(value = "0.00")
    @Digits(integer = 13, fraction = 0)
    private BigDecimal amount;
}
