package com.finsight.backend.dto.response;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

/**
 * Summary figures for the dashboard (income, expense, savings and counts). NOTE: not
 * referenced by any controller or service at the moment.
 */
@Getter
@Setter
public class DashboardResponse {

    private BigDecimal totalIncome;

    private BigDecimal totalExpense;

    private BigDecimal totalSavings;

    private Integer recurringSubscriptions;

    private Integer unusualTransactions;

    private Integer recommendationCount;

}