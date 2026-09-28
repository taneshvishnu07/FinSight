/**
 * FinSight File Notes: Exposes REST API endpoints for transaction analysis operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;
import com.finsight.backend.service.TransactionAnalysisService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
@RequiredArgsConstructor
public class TransactionAnalysisController {

    private final TransactionAnalysisService transactionAnalysisService;


    /*
     * ============================================================
     * RUN ANALYSIS
     * ============================================================
     *
     * POST /api/analysis/upload/{uploadHistoryId}
     *
     * Example:
     *
     * POST /api/analysis/upload/3
     *
     */
    @PostMapping("/uploads/{uploadHistoryId}")
    public ResponseEntity<TransactionAnalysisResponse>
            analyseUpload(
                    @PathVariable Long uploadHistoryId) {

        TransactionAnalysisResponse response =
                transactionAnalysisService
                        .analyseUpload(
                                uploadHistoryId
                        );

        return ResponseEntity.ok(
                response
        );
    }


    /*
     * ============================================================
     * GET ANALYSIS BY ID
     * ============================================================
     *
     * GET /api/analysis/{analysisId}
     *
     */
    @GetMapping("/{analysisId}")
    public ResponseEntity<TransactionAnalysisResponse>
            getAnalysisById(
                    @PathVariable Long analysisId) {

        TransactionAnalysisResponse response =
                transactionAnalysisService
                        .getAnalysisById(
                                analysisId
                        );

        return ResponseEntity.ok(
                response
        );
    }


    /*
     * ============================================================
     * GET LATEST ANALYSIS
     * ============================================================
     *
     * GET /api/analysis/latest
     *
     */
    @GetMapping("/latest")
    public ResponseEntity<TransactionAnalysisResponse>
            getLatestAnalysis() {

        TransactionAnalysisResponse response =
                transactionAnalysisService
                        .getLatestAnalysis();

        return ResponseEntity.ok(
                response
        );
    }
}