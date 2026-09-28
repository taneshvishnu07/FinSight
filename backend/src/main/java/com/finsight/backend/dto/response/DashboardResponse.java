/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.response;

import java.math.BigDecimal;

import lombok.Getter;
import lombok.Setter;

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