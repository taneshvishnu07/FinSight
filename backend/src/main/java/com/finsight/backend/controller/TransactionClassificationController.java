/**
 * FinSight File Notes: Exposes REST API endpoints for transaction classification operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finsight.backend.dto.classification.ClassificationResponse;
import com.finsight.backend.service.TransactionClassificationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/classification")
@RequiredArgsConstructor
public class TransactionClassificationController {

    private final TransactionClassificationService transactionClassificationService;

    @PostMapping("/{uploadHistoryId}")
    public ResponseEntity<ClassificationResponse> classifyUpload(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                transactionClassificationService.classifyUpload(uploadHistoryId)
        );
    }
}
