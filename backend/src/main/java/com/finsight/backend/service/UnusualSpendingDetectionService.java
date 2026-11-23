package com.finsight.backend.service;

import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;

public interface UnusualSpendingDetectionService {

    AnomalyDetectionResponse detectAnomalies(Long uploadHistoryId);
}