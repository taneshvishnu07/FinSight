package com.finsight.backend.entity;

import com.finsight.backend.enums.OccupationType;
import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * The user's financial setup (table `financial_profiles`), created once after
 * registration. Holds occupation type (student/employee/other) with the matching
 * details entity, the savings goal period, and parents all per-user data: monthly
 * budgets, upload history, transactions and analyses. Deleting a profile cascades to
 * all of them.
 */
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

    // Savings goal horizon in months (default 6), used by the recommendation and alert
    // rules.
    @Column(nullable = false)
    private Integer financialGoalMonths = 6;

    // Only one of studentDetails / employeeDetails is normally filled, depending on
    // occupationType.
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
