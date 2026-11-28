package com.finsight.backend.service;

import com.finsight.backend.dto.transaction.TransactionCreateRequest;
import com.finsight.backend.dto.transaction.TransactionResponse;
import com.finsight.backend.dto.transaction.TransactionUpdateRequest;

import java.util.List;

/**
 * CRUD on the logged-in user's transactions. Implementation: TransactionServiceImpl.
 */
public interface TransactionService {

    /**
     * Add a transaction manually.
     */
    TransactionResponse createTransaction(
            TransactionCreateRequest request
    );

    /**
     * All of the user's transactions, newest first.
     */
    List<TransactionResponse> getMyTransactions();

    /**
     * One transaction (must belong to the user).
     */
    TransactionResponse getTransactionById(
            Long transactionId
    );

    /**
     * Update a transaction (must belong to the user).
     */
    TransactionResponse updateTransaction(
            Long transactionId,
            TransactionUpdateRequest request
    );

    /**
     * Delete a transaction (must belong to the user).
     */
    void deleteTransaction(
            Long transactionId
    );
}

