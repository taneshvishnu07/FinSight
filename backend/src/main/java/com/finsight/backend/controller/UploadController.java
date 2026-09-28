/**
 * FinSight File Notes: Exposes REST API endpoints for upload operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.finsight.backend.dto.response.UploadResponse;
import com.finsight.backend.dto.transaction.TransactionResponse;
import com.finsight.backend.service.UploadService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

    private final UploadService uploadService;


    // ============================================================
    // UPLOAD TRANSACTION CSV
    // POST /api/uploads/transactions
    // ============================================================

    @PostMapping(
            value = "/transactions",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<UploadResponse> uploadTransactions(
            @RequestPart("file") MultipartFile file) {

        UploadResponse response =
                uploadService.uploadTransactions(file);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }


    // ============================================================
    // GET ALL UPLOAD HISTORY
    // GET /api/uploads
    // ============================================================

    @GetMapping
    public ResponseEntity<List<UploadResponse>> getMyUploads() {

        return ResponseEntity.ok(
                uploadService.getMyUploads()
        );
    }


    // ============================================================
    // GET ONE UPLOAD
    // GET /api/uploads/{id}
    // ============================================================

    @GetMapping("/{id}")
    public ResponseEntity<UploadResponse> getUploadById(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                uploadService.getUploadById(id)
        );
    }


    // ============================================================
    // GET TRANSACTIONS FROM ONE UPLOAD
    // GET /api/uploads/{id}/transactions
    // ============================================================

    @GetMapping("/{id}/transactions")
    public ResponseEntity<List<TransactionResponse>>
            getTransactionsByUpload(
                    @PathVariable Long id) {

        return ResponseEntity.ok(
                uploadService.getTransactionsByUpload(id)
        );
    }
}