package com.finsight.backend.service;

import com.finsight.backend.dto.classification.ClassificationResponse;

/**
 * AI categorisation of transactions. Implementation:
 * TransactionClassificationServiceImpl.
 */
public interface TransactionClassificationService {

    /**
     * Classify the upload's transactions with the AI service and save the categories.
     */
    ClassificationResponse classifyUpload(Long uploadHistoryId);
}
