/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.SpendingAlert;

public interface SpendingAlertRepository extends JpaRepository<SpendingAlert, Long> {

    List<SpendingAlert> findByFinancialAnalysis(
            FinancialAnalysis financialAnalysis);

    Optional<SpendingAlert> findByIdAndFinancialAnalysis(
            Long id,
            FinancialAnalysis financialAnalysis);

}