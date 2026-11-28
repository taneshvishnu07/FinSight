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
 * Extra profile data for employees (table `employee_details`): occupation, company,
 * monthly salary and side-income flag. The salary is compared with recorded income by
 * the AI service rules.
 */
@Entity
@Table(name = "employee_details")
public class EmployeeDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "financial_profile_id",
            nullable = false,
            unique = true
    )
    private FinancialProfile financialProfile;

    @Column(nullable = false)
    private String occupation;

    @Column(nullable = false)
    private String companyName;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlySalary = BigDecimal.ZERO;

    @Column(nullable = false)
    private Boolean hasSideIncome = false;

}