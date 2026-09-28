/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
 List<MonthlyBudget> findAllByFinancialProfileOrderByBudgetMonthAsc(FinancialProfile profile);
 Optional<MonthlyBudget> findByFinancialProfileAndBudgetMonth(FinancialProfile profile, String budgetMonth);
}
