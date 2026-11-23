package com.finsight.backend.service;

import com.finsight.backend.dto.forecast.ForecastResponse;

public interface ForecastService {

    ForecastResponse forecast(Long uploadHistoryId, int monthsAhead);
}
