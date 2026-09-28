/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service;

import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;

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