/**
 * FinSight File Notes: Exposes REST API endpoints for unusual spending operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.service.UnusualSpendingDetectionService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class UnusualSpendingController {

    private final UnusualSpendingDetectionService
            unusualSpendingDetectionService;


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