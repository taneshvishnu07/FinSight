/**
 * FinSight File Notes: Exposes REST API endpoints for alert operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finsight.backend.dto.alert.AlertResponse;
import com.finsight.backend.service.AlertService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @PostMapping("/generate/{uploadHistoryId}")
    public ResponseEntity<AlertResponse> generateAlerts(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                alertService.generateAlerts(uploadHistoryId)
        );
    }

    @GetMapping("/{uploadHistoryId}")
    public ResponseEntity<AlertResponse> getAlerts(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                alertService.getAlerts(uploadHistoryId)
        );
    }

    @PatchMapping("/{uploadHistoryId}/{alertId}/read")
    public ResponseEntity<AlertResponse> markAsRead(
            @PathVariable Long uploadHistoryId,
            @PathVariable Long alertId) {

        return ResponseEntity.ok(
                alertService.markAsRead(uploadHistoryId, alertId)
        );
    }

    @PostMapping("/{uploadHistoryId}/read-all")
    public ResponseEntity<AlertResponse> markAllAsRead(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                alertService.markAllAsRead(uploadHistoryId)
        );
    }
}
