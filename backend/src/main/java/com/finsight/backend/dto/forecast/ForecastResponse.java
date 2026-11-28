package com.finsight.backend.dto.forecast;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Expense forecast (mirrors the Python /api/forecast JSON): the model used and
 * predicted expense with lower/upper bounds per future month.
 */
@Getter
@Setter
@NoArgsConstructor
public class ForecastResponse {

    private String status = "SUCCESS";
    private String message = "";
    private String model = "NONE";
    private int historicalMonthCount;
    private BigDecimal historicalAverageMonthlyExpense = BigDecimal.ZERO;
    private List<ForecastMonth> forecastMonths = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ForecastMonth {
        private String month = "";
        private BigDecimal predictedExpense = BigDecimal.ZERO;
        private BigDecimal lowerBound = BigDecimal.ZERO;
        private BigDecimal upperBound = BigDecimal.ZERO;
    }
}
