/**
 * FinSight File Notes: Stores the user financial profile used to personalise financial analysis.
 */
package com.finsight.backend.entity;

import com.finsight.backend.enums.OccupationType;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "financial_profiles")
public class FinancialProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OccupationType occupationType;

    @Column(nullable = false)
    private Integer financialGoalMonths = 6;

    @OneToOne(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private StudentDetails studentDetails;

    @OneToOne(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private EmployeeDetails employeeDetails;

    @OneToMany(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("budgetMonth ASC")
    private List<MonthlyBudget> monthlyBudgets = new ArrayList<>();

    @OneToMany(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UploadHistory> uploadHistories = new ArrayList<>();

    @OneToMany(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Transaction> transactions = new ArrayList<>();

    @OneToMany(mappedBy = "financialProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FinancialAnalysis> analyses = new ArrayList<>();
}
