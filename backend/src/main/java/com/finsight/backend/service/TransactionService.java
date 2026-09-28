/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service;

import com.finsight.backend.dto.transaction.TransactionCreateRequest;
import com.finsight.backend.dto.transaction.TransactionResponse;
import com.finsight.backend.dto.transaction.TransactionUpdateRequest;

import java.util.List;

public interface TransactionService {

    TransactionResponse createTransaction(
            TransactionCreateRequest request
    );

    List<TransactionResponse> getMyTransactions();

    TransactionResponse getTransactionById(
            Long transactionId
    );

    TransactionResponse updateTransaction(
            Long transactionId,
            TransactionUpdateRequest request
    );

    void deleteTransaction(
            Long transactionId
    );
}

