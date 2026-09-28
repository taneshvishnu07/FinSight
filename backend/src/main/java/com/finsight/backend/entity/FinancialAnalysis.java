/**
 * FinSight File Notes: Stores the main financial-analysis summary produced for a completed job.
 */
package com.finsight.backend.entity;

import com.finsight.backend.enums.AnalysisStatus;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(
        name = "financial_analysis",
        indexes = {
                @Index(name = "idx_financial_analysis_profile_id", columnList = "financial_profile_id")
        }
)
public class FinancialAnalysis extends BaseEntity {

   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "financial_profile_id", nullable = false)
   private FinancialProfile financialProfile;

   @OneToOne(fetch = FetchType.LAZY)
   @JoinColumn(
        name = "analysis_job_id",
        nullable = false,
        unique = true
    )
    private AnalysisJob analysisJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(nullable = false)
    private LocalDateTime analysedAt = LocalDateTime.now();
    
    @Column(length = 1000)
    private String summary;

    // ==========================
    // Financial Summary
    // ==========================

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalIncome = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalExpense = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalSavings = BigDecimal.ZERO;

    // ==========================
    // Spending Analysis
    // ==========================

    @Column(nullable = false)
    private Integer recurringSubscriptions = 0;

    @Column(nullable = false)
    private Integer unusualTransactions = 0;

    @Column(nullable = false)
    private Integer recommendationCount = 0;

    // ==========================
    // AI Output
    // ==========================

    @OneToMany(
            mappedBy = "financialAnalysis",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<Recommendation> recommendations = new ArrayList<>();

    @OneToMany(
            mappedBy = "financialAnalysis",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private List<SpendingAlert> alerts = new ArrayList<>();

}