/**
 * FinSight File Notes: Represents one month-specific budget linked to a user financial profile.
 */
package com.finsight.backend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor
@Entity @Table(name="monthly_budgets", uniqueConstraints=@UniqueConstraint(columnNames={"financial_profile_id","budget_month"}))
public class MonthlyBudget extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="financial_profile_id", nullable=false)
    private FinancialProfile financialProfile;
    @Column(name="budget_month", nullable=false, length=7)
    private String budgetMonth;
    @Column(nullable=false, precision=15, scale=2)
    private BigDecimal amount;
}
