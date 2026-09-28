/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;

import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransactionRepository
        extends JpaRepository<Transaction, Long> {


    // ============================================================
    // GET ALL TRANSACTIONS FOR USER
    // ============================================================

    List<Transaction> findByUserOrderByTransactionDateDesc(
            User user
    );


    // ============================================================
    // GET ONE TRANSACTION FOR USER
    // ============================================================

    Optional<Transaction> findByIdAndUser(
            Long id,
            User user
    );


    // ============================================================
    // GET TRANSACTIONS BELONGING TO ONE UPLOAD
    // ============================================================

    List<Transaction>
    findByUploadHistoryAndUserOrderByTransactionDateDesc(
            UploadHistory uploadHistory,
            User user
    );
}