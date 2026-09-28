/**
 * FinSight File Notes: Exposes REST API endpoints for forecast operations used by the FinSight frontend and services.
 */
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

@RestController
@RequestMapping("/api/forecast")
@RequiredArgsConstructor
public class ForecastController {

    private final ForecastService forecastService;

    @PostMapping("/{uploadHistoryId}")
    public ResponseEntity<ForecastResponse> forecast(
            @PathVariable Long uploadHistoryId,
            @RequestParam(defaultValue = "1") int monthsAhead) {

        return ResponseEntity.ok(
                forecastService.forecast(uploadHistoryId, monthsAhead)
        );
    }
}
