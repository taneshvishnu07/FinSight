package com.finsight.backend.service;

import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;

/**
 * Builds and stores the per-upload analysis (income/expense/savings totals,
 * subscription and unusual-spending counts, recommendations and alerts).
 * Implementation: TransactionAnalysisServiceImpl.
 */
public interface TransactionAnalysisService {

    /*
     * Run analysis for one uploaded transaction file.
     */
    TransactionAnalysisResponse analyseUpload(
            Long uploadHistoryId
    );

    /*
     * Get one analysis.
     */
    TransactionAnalysisResponse getAnalysisById(
            Long analysisId
    );

    /*
     * Get the latest analysis for the authenticated user.
     */
    TransactionAnalysisResponse getLatestAnalysis();

}