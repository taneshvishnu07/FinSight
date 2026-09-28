/**
 * FinSight File Notes: Stores one financial alert generated from an analysis.
 */
package com.finsight.backend.entity;

import com.finsight.backend.enums.AlertSeverity;
import com.finsight.backend.enums.AlertType;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

@Entity
@Table(name = "spending_alerts")
public class SpendingAlert extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id", nullable = false)
    private FinancialAnalysis financialAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AlertSeverity severity;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(nullable = false)
    private String generatedBy;

    @Column(nullable = false)
    private Boolean isRead = false;

}