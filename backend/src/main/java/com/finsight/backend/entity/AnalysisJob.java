package com.finsight.backend.entity;

import com.finsight.backend.enums.AnalysisStatus;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor

/**
 * One run of the analysis pipeline for an uploaded file (table `analysis_jobs`):
 * tracks status (PENDING -> PROCESSING -> COMPLETED/FAILED), timing and any error
 * message. Its result is stored as a FinancialAnalysis.
 */
@Entity
@Table(name = "analysis_jobs")
public class AnalysisJob extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "upload_history_id", nullable = false)
    private UploadHistory uploadHistory;

    @OneToOne(
            mappedBy = "analysisJob",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private FinancialAnalysis financialAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String errorMessage;

}