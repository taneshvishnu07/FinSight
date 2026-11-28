package com.finsight.backend.controller;

import com.finsight.backend.dto.orchestrator.FinancialAnalysisOrchestratorResponse;
import com.finsight.backend.service.FinancialAnalysisOrchestratorService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Single entry point that runs the whole analysis pipeline (classification,
 * subscriptions, unusual spending, forecast, recommendations, alerts) for one upload.
 * The frontend calls this after a file is uploaded. Mounted under /api/financial-
 * analysis.
 */
@RestController
@RequestMapping("/api/financial-analysis")
@RequiredArgsConstructor
public class FinancialAnalysisOrchestratorController {

    private final FinancialAnalysisOrchestratorService
            financialAnalysisOrchestratorService;


    /*
     * ============================================================
     * RUN COMPLETE FINANCIAL ANALYSIS
     * ============================================================
     *
     * POST
     *
     * /api/financial-analysis/run/{uploadHistoryId}
     *
     * Example:
     *
     * POST /api/financial-analysis/run/1
     *
     * ============================================================
     */

    @PostMapping("/run/{uploadHistoryId}")
    public ResponseEntity<FinancialAnalysisOrchestratorResponse>
            runFinancialAnalysis(
                    @PathVariable Long uploadHistoryId) {

        FinancialAnalysisOrchestratorResponse response =
                financialAnalysisOrchestratorService
                        .runFinancialAnalysis(
                                uploadHistoryId
                        );

        return ResponseEntity.ok(
                response
        );
    }
}