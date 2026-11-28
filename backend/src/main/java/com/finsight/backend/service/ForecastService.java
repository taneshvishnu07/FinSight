package com.finsight.backend.service;

import com.finsight.backend.dto.forecast.ForecastResponse;

/**
 * Expense forecasting via the AI service. Implementation: ForecastServiceImpl.
 */
public interface ForecastService {

    /**
     * Predict the next `monthsAhead` months of expenses for the upload.
     */
    ForecastResponse forecast(Long uploadHistoryId, int monthsAhead);
}
