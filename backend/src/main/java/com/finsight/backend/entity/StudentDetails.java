package com.finsight.backend.entity;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor

/**
 * Extra profile data for students (table `student_details`): institution, course,
 * study year, monthly allowance and scholarship flag. The allowance feeds the
 * "spending above allowance" rules in the AI service.
 */
@Entity
@Table(name = "student_details")
public class StudentDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "financial_profile_id",
            nullable = false,
            unique = true
    )
    private FinancialProfile financialProfile;

    @Column(nullable = false)
    private String institution;

    @Column(nullable = false)
    private String course;

    @Column(nullable = false)
    private Integer studyYear;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyAllowance = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean receivesScholarship = false;

}