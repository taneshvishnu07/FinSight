package com.finsight.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finsight.backend.dto.insight.FinancialInsightResponse;
import com.finsight.backend.service.FinancialInsightService;

import lombok.RequiredArgsConstructor;

/**
 * Financial health insights for the dashboard and report pages (score, status and
 * prioritised insights), mounted under /api/insights.
 */
@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class FinancialInsightController {

    private final FinancialInsightService financialInsightService;


    /**
     * GET /api/insights - build the insights for the logged-in user from their saved
     * data. The financial health score is calculated here on the backend; the frontend
     * only displays it.
     */
    @GetMapping
    public ResponseEntity<FinancialInsightResponse> generateInsights() {

        FinancialInsightResponse response =
                financialInsightService.generateInsights();

        return ResponseEntity.ok(
                response
        );
    }
}