package com.finsight.backend.client;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.finsight.backend.entity.EmployeeDetails;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.MonthlyBudget;
import com.finsight.backend.entity.StudentDetails;

/**
 * Converts a FinancialProfile entity into the plain JSON map the AI service expects as
 * `financialProfile` (occupation, savings goal, monthly budgets and the student or
 * employee details). Utility class - static methods only.
 */
public final class AiFinancialProfilePayloadBuilder {

    private AiFinancialProfilePayloadBuilder() {
    }

    /**
     * Null-safe conversion; returns an empty map for a null profile. Keys match what
     * the Python recommendation/alert agents read (e.g. monthlyAllowance,
     * monthlySalary, monthlyBudgets).
     */
    public static Map<String, Object> build(FinancialProfile profile) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (profile == null) {
            return payload;
        }

        payload.put("occupationType", profile.getOccupationType() == null ? "" : profile.getOccupationType().name());
        payload.put("financialGoalMonths", profile.getFinancialGoalMonths() == null ? 6 : profile.getFinancialGoalMonths());

        List<Map<String, Object>> monthlyBudgets = new ArrayList<>();
        if (profile.getMonthlyBudgets() != null) {
            for (MonthlyBudget budget : profile.getMonthlyBudgets()) {
                if (budget == null || budget.getBudgetMonth() == null || budget.getAmount() == null) {
                    continue;
                }
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("budgetMonth", budget.getBudgetMonth());
                item.put("amount", budget.getAmount());
                monthlyBudgets.add(item);
            }
        }
        payload.put("monthlyBudgets", monthlyBudgets);

        StudentDetails student = profile.getStudentDetails();
        if (student != null) {
            payload.put("institution", text(student.getInstitution()));
            payload.put("course", text(student.getCourse()));
            payload.put("studyYear", student.getStudyYear());
            payload.put("monthlyAllowance", number(student.getMonthlyAllowance()));
            payload.put("receivesScholarship", Boolean.TRUE.equals(student.getReceivesScholarship()));
        }

        EmployeeDetails employee = profile.getEmployeeDetails();
        if (employee != null) {
            payload.put("occupation", text(employee.getOccupation()));
            payload.put("companyName", text(employee.getCompanyName()));
            payload.put("monthlySalary", number(employee.getMonthlySalary()));
            payload.put("hasSideIncome", Boolean.TRUE.equals(employee.getHasSideIncome()));
        }

        return payload;
    }

    /**
     * Null becomes zero.
     */
    private static BigDecimal number(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * Null becomes an empty string.
     */
    private static String text(String value) {
        return value == null ? "" : value;
    }
}
