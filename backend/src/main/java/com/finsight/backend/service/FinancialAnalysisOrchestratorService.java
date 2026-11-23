package com.finsight.backend.service;

import com.finsight.backend.dto.orchestrator.FinancialAnalysisOrchestratorResponse;

public interface FinancialAnalysisOrchestratorService {

    FinancialAnalysisOrchestratorResponse
            runFinancialAnalysis(
                    Long uploadHistoryId
            );
}