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

/**
 * REST endpoints for spending alerts, mounted under /api/alerts. Alerts belong to the
 * latest analysis of an upload (`uploadHistoryId`). Thin controller - logic is in
 * AlertService. All data is scoped to the logged-in user; a user can only act on their
 * own uploads.
 */
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * POST /api/alerts/generate/{uploadHistoryId} - (re)generate alerts for the upload
     * by calling the AI service, and save them.
     */
    @PostMapping("/generate/{uploadHistoryId}")
    public ResponseEntity<AlertResponse> generateAlerts(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                alertService.generateAlerts(uploadHistoryId)
        );
    }

    /**
     * GET /api/alerts/{uploadHistoryId} - read the saved alerts for the upload.
     */
    @GetMapping("/{uploadHistoryId}")
    public ResponseEntity<AlertResponse> getAlerts(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                alertService.getAlerts(uploadHistoryId)
        );
    }

    /**
     * PATCH /api/alerts/{uploadHistoryId}/{alertId}/read - mark one alert as read.
     */
    @PatchMapping("/{uploadHistoryId}/{alertId}/read")
    public ResponseEntity<AlertResponse> markAsRead(
            @PathVariable Long uploadHistoryId,
            @PathVariable Long alertId) {

        return ResponseEntity.ok(
                alertService.markAsRead(uploadHistoryId, alertId)
        );
    }

    /**
     * POST /api/alerts/{uploadHistoryId}/read-all - mark every alert of the upload as
     * read.
     */
    @PostMapping("/{uploadHistoryId}/read-all")
    public ResponseEntity<AlertResponse> markAllAsRead(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                alertService.markAllAsRead(uploadHistoryId)
        );
    }
}
