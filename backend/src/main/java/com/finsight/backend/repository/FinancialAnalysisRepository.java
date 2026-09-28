/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.FinancialProfile;

public interface FinancialAnalysisRepository extends JpaRepository<FinancialAnalysis, Long> {

    List<FinancialAnalysis> findByFinancialProfileOrderByAnalysedAtDesc(
            FinancialProfile financialProfile);

    Optional<FinancialAnalysis> findFirstByFinancialProfileAndStatusOrderByAnalysedAtDesc(
            FinancialProfile financialProfile,
            com.finsight.backend.enums.AnalysisStatus status);

    Optional<FinancialAnalysis> findFirstByFinancialProfileOrderByAnalysedAtDesc(
            FinancialProfile financialProfile);

}