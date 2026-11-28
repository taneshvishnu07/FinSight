package com.finsight.backend.controller;

import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.service.UnusualSpendingDetectionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Unusual (anomalous) spending detection, mounted under /api/anomalies.
 */
@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class UnusualSpendingController {

    private final UnusualSpendingDetectionService
            unusualSpendingDetectionService;


    /**
     * POST /api/anomalies/detect/{uploadHistoryId} - find unusually large expenses in
     * the upload using the AI service.
     */
    @PostMapping("/detect/{uploadHistoryId}")
    public ResponseEntity<AnomalyDetectionResponse>
            detectAnomalies(
                    @PathVariable Long uploadHistoryId) {

        AnomalyDetectionResponse response =
                unusualSpendingDetectionService
                        .detectAnomalies(
                                uploadHistoryId
                        );

        return ResponseEntity.ok(
                response
        );
    }
}