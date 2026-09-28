/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.financialprofile;

import com.finsight.backend.dto.budget.MonthlyBudgetRequest;
import com.finsight.backend.enums.OccupationType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class FinancialProfileCreateRequest {

    @NotNull
    private OccupationType occupationType;

    @NotNull
    @Min(1)
    private Integer financialGoalMonths;

    /**
     * Monthly budgets are stored independently by YYYY-MM. Keeping them in
     * the profile request lets first-time setup save personal information and
     * budget values in one transaction.
     */
    @Valid
    private List<@Valid MonthlyBudgetRequest> monthlyBudgets = new ArrayList<>();

    // =========================
    // STUDENT DETAILS
    // =========================

    private String institution;
    private String course;
    private Integer studyYear;
    private java.math.BigDecimal monthlyAllowance;
    private Boolean receivesScholarship;

    // =========================
    // EMPLOYEE / OTHER DETAILS
    // =========================

    private String occupation;
    private String companyName;
    private java.math.BigDecimal monthlySalary;
    private Boolean hasSideIncome;
}
