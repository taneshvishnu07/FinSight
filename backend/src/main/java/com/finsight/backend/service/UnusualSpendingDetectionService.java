package com.finsight.backend.service;

import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;

/**
 * Finds unusually large expenses through the AI service. Implementation:
 * UnusualSpendingDetectionServiceImpl.
 */
public interface UnusualSpendingDetectionService {

    /**
     * Detect unusual expenses in the upload's transactions.
     */
    AnomalyDetectionResponse detectAnomalies(Long uploadHistoryId);
}