/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service;

import com.finsight.backend.dto.classification.ClassificationResponse;

public interface TransactionClassificationService {

    ClassificationResponse classifyUpload(Long uploadHistoryId);
}
