/**
 * FinSight File Notes: Exposes REST API endpoints for transaction operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import com.finsight.backend.dto.transaction.TransactionCreateRequest;
import com.finsight.backend.dto.transaction.TransactionResponse;
import com.finsight.backend.dto.transaction.TransactionUpdateRequest;
import com.finsight.backend.service.TransactionService;

import jakarta.validation.Valid;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;


    // ============================================================
    // CREATE TRANSACTION
    // POST /api/transactions
    // ============================================================
    @PostMapping
    public ResponseEntity<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionCreateRequest request) {

        TransactionResponse response =
                transactionService.createTransaction(request);

        return ResponseEntity.ok(response);
    }


    // ============================================================
    // GET ALL TRANSACTIONS
    // GET /api/transactions
    // ============================================================
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getMyTransactions() {

        List<TransactionResponse> transactions =
                transactionService.getMyTransactions();

        return ResponseEntity.ok(transactions);
    }


    // ============================================================
    // GET ONE TRANSACTION
    // GET /api/transactions/{id}
    // ============================================================
    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransactionById(
            @PathVariable Long id) {

        TransactionResponse response =
                transactionService.getTransactionById(id);

        return ResponseEntity.ok(response);
    }


    // ============================================================
    // UPDATE TRANSACTION
    // PUT /api/transactions/{id}
    // ============================================================
    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody TransactionUpdateRequest request) {

        TransactionResponse response =
                transactionService.updateTransaction(
                        id,
                        request
                );

        return ResponseEntity.ok(response);
    }


    // ============================================================
    // DELETE TRANSACTION
    // DELETE /api/transactions/{id}
    // ============================================================
    @DeleteMapping("/{id}")
        public ResponseEntity<Void> deleteTransaction(
                @PathVariable Long id) {

        transactionService.deleteTransaction(id);

        return ResponseEntity.noContent().build();
   }
}