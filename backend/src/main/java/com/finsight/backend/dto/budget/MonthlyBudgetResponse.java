package com.finsight.backend.dto.budget;
import lombok.AllArgsConstructor; import lombok.Getter;
import java.math.BigDecimal;
/**
 * One saved monthly budget.
 */
@Getter @AllArgsConstructor
public class MonthlyBudgetResponse { private Long id; private String budgetMonth; private BigDecimal amount; }
