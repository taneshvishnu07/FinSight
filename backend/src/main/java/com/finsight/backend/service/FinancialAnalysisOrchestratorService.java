package com.finsight.backend.service;

import com.finsight.backend.dto.orchestrator.FinancialAnalysisOrchestratorResponse;

/**
 * Runs the complete analysis pipeline for one upload in a fixed order and combines the
 * results. Implementation: FinancialAnalysisOrchestratorServiceImpl.
 */
public interface FinancialAnalysisOrchestratorService {

    /**
     * Execute every analysis step for the upload and return the combined response.
     */
    FinancialAnalysisOrchestratorResponse
            runFinancialAnalysis(
                    Long uploadHistoryId
            );
}