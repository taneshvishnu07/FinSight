package com.finsight.backend.service;

import com.finsight.backend.dto.classification.ClassificationResponse;

public interface TransactionClassificationService {

    ClassificationResponse classifyUpload(Long uploadHistoryId);
}
