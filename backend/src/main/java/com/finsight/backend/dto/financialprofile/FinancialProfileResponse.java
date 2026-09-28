/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.financialprofile;

import com.finsight.backend.dto.budget.MonthlyBudgetResponse;
import com.finsight.backend.enums.OccupationType;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class FinancialProfileResponse {

    private Long id;
    private OccupationType occupationType;
    private Integer financialGoalMonths;

    // Student details
    private String institution;
    private String course;
    private Integer studyYear;
    private BigDecimal monthlyAllowance;
    private Boolean receivesScholarship;

    // Employee / Other details
    private String occupation;
    private String companyName;
    private BigDecimal monthlySalary;
    private Boolean hasSideIncome;

    // Monthly budgets stored independently by month.
    private List<MonthlyBudgetResponse> monthlyBudgets;
}
