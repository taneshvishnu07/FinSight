package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.AnalysisJob;
import com.finsight.backend.entity.UploadHistory;

/**
 * Data access for AnalysisJob. Queries are always scoped to an UploadHistory so a user
 * can only reach jobs of their own uploads.
 */
public interface AnalysisJobRepository
        extends JpaRepository<AnalysisJob, Long> {

    /**
     * All analysis jobs of one upload, newest first.
     */
    List<AnalysisJob> findByUploadHistoryOrderByStartedAtDesc(
            UploadHistory uploadHistory
    );

    /**
     * One job by id, only if it belongs to the given upload (ownership check).
     */
    Optional<AnalysisJob> findByIdAndUploadHistory(
            Long id,
            UploadHistory uploadHistory
    );
}