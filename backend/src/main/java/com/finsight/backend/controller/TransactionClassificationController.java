package com.finsight.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.finsight.backend.dto.classification.ClassificationResponse;
import com.finsight.backend.service.TransactionClassificationService;

import lombok.RequiredArgsConstructor;

/**
 * Triggers AI category classification for the transactions of an upload, mounted under
 * /api/classification.
 */
@RestController
@RequestMapping("/api/classification")
@RequiredArgsConstructor
public class TransactionClassificationController {

    private final TransactionClassificationService transactionClassificationService;

    /**
     * POST /api/classification/{uploadHistoryId} - classify the upload's transactions
     * with the AI service and save the categories.
     */
    @PostMapping("/{uploadHistoryId}")
    public ResponseEntity<ClassificationResponse> classifyUpload(
            @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                transactionClassificationService.classifyUpload(uploadHistoryId)
        );
    }
}
