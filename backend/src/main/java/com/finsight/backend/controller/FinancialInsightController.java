/**
 * FinSight File Notes: Exposes REST API endpoints for financial insight operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finsight.backend.dto.insight.FinancialInsightResponse;
import com.finsight.backend.service.FinancialInsightService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/insights")
@RequiredArgsConstructor
public class FinancialInsightController {

    private final FinancialInsightService financialInsightService;


    @GetMapping
    public ResponseEntity<FinancialInsightResponse> generateInsights() {

        FinancialInsightResponse response =
                financialInsightService.generateInsights();

        return ResponseEntity.ok(
                response
        );
    }
}