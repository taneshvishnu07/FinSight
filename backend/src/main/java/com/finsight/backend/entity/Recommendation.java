/**
 * FinSight File Notes: Stores one personalised recommendation generated from an analysis.
 */
package com.finsight.backend.entity;

import java.math.BigDecimal;

import com.finsight.backend.enums.RecommendationPriority;
import com.finsight.backend.enums.RecommendationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "recommendations")
public class Recommendation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "analysis_id",
            nullable = false
    )
    private FinancialAnalysis financialAnalysis;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationType type;


    @Column(nullable = false)
    private String title;


    @Column(nullable = false, length = 1000)
    private String description;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RecommendationPriority priority;


    @Column(
            precision = 15,
            scale = 2
    )
    private BigDecimal expectedSavings;


    @Column(nullable = false)
    private String generatedBy;


    @Column(nullable = false)
    private Boolean accepted = false;


    @Column(nullable = false)
    private Boolean dismissed = false;


    /*
     * Additional recommendation context.
     */

    @Column(
            name = "related_category",
            length = 100
    )
    private String relatedCategory;


    @Column(
            name = "related_merchant",
            length = 255
    )
    private String relatedMerchant;
}