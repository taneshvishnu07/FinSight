/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.budget;
import lombok.AllArgsConstructor; import lombok.Getter;
import java.math.BigDecimal;
@Getter @AllArgsConstructor
public class MonthlyBudgetResponse { private Long id; private String budgetMonth; private BigDecimal amount; }
