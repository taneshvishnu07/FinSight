/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.AnalysisJob;
import com.finsight.backend.entity.UploadHistory;

public interface AnalysisJobRepository
        extends JpaRepository<AnalysisJob, Long> {

    List<AnalysisJob> findByUploadHistoryOrderByStartedAtDesc(
            UploadHistory uploadHistory
    );

    Optional<AnalysisJob> findByIdAndUploadHistory(
            Long id,
            UploadHistory uploadHistory
    );
}