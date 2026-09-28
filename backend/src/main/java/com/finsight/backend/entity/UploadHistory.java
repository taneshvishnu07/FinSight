/**
 * FinSight File Notes: Records an uploaded transaction file and its processing status.
 */
package com.finsight.backend.entity;

import com.finsight.backend.enums.UploadStatus;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(name = "upload_history")
public class UploadHistory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "financial_profile_id", nullable = false)
    private FinancialProfile financialProfile;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private Integer totalRecords;

    @Column(nullable = false)
    private Integer importedRecords = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UploadStatus status = UploadStatus.UPLOADED;

    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();

    @OneToMany(
            mappedBy = "uploadHistory",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(
            mappedBy = "uploadHistory",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<AnalysisJob> analysisJobs = new ArrayList<>();

}