/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.UploadHistory;

public interface UploadHistoryRepository
        extends JpaRepository<UploadHistory, Long> {

    /*
     * Find a specific upload belonging to a financial profile.
     */
    Optional<UploadHistory> findByIdAndFinancialProfile(
            Long id,
            FinancialProfile financialProfile
    );

    /*
     * Find all uploads belonging to a financial profile,
     * newest upload first.
     */
    List<UploadHistory> findByFinancialProfileOrderByUploadedAtDesc(
            FinancialProfile financialProfile
    );

    /*
     * Find the most recent upload belonging to a financial profile.
     */
    Optional<UploadHistory> findTopByFinancialProfileOrderByUploadedAtDesc(
            FinancialProfile financialProfile
    );
}