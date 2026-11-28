package com.finsight.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.finsight.backend.dto.forecast.ForecastResponse;
import com.finsight.backend.service.ForecastService;

import lombok.RequiredArgsConstructor;

/**
 * Expense forecasting endpoint, mounted under /api/forecast.
 */
@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    /**
     * POST /api/forecast/{uploadHistoryId}?monthsAhead=N - predict the next N months
     * of expenses (default 1) for the upload.
     */
    @PostMapping("/{uploadHistoryId}")
    public ResponseEntity<ForecastResponse> forecast(
            @PathVariable Long uploadHistoryId,
            @RequestParam(defaultValue = "1") int monthsAhead) {

        return ResponseEntity.ok(
                forecastService.forecast(uploadHistoryId, monthsAhead)
        );
    }
}
