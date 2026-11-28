package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.SpendingAlert;

/**
 * Data access for SpendingAlert.
 */
public interface SpendingAlertRepository extends JpaRepository<SpendingAlert, Long> {

    List<SpendingAlert> findByFinancialAnalysis(
            FinancialAnalysis financialAnalysis);

    /**
     * One alert by id, only if it belongs to the given analysis (ownership check, used
     * when marking alerts read).
     */
    Optional<SpendingAlert> findByIdAndFinancialAnalysis(
            Long id,
            FinancialAnalysis financialAnalysis);

}