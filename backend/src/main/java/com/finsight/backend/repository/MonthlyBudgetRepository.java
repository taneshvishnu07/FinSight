package com.finsight.backend.repository;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.MonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
/**
 * Data access for MonthlyBudget.
 */
public interface MonthlyBudgetRepository extends JpaRepository<MonthlyBudget, Long> {
 /**
  * All budgets of a profile in calendar order (oldest month first).
  */
 List<MonthlyBudget> findAllByFinancialProfileOrderByBudgetMonthAsc(FinancialProfile profile);
 /**
  * Budget for one month; budgetMonth is "YYYY-MM".
  */
 Optional<MonthlyBudget> findByFinancialProfileAndBudgetMonth(FinancialProfile profile, String budgetMonth);
}
