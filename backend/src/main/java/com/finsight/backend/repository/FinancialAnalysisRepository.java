package com.finsight.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.FinancialProfile;

/**
 * Data access for FinancialAnalysis results. Spring Data builds the queries from the
 * method names.
 */
public interface FinancialAnalysisRepository extends JpaRepository<FinancialAnalysis, Long> {

    /**
     * All analyses of a profile, newest first.
     */
    List<FinancialAnalysis> findByFinancialProfileOrderByAnalysedAtDesc(
            FinancialProfile financialProfile);

    /**
     * The latest analysis with the given status (e.g. the latest COMPLETED one).
     */
    Optional<FinancialAnalysis> findFirstByFinancialProfileAndStatusOrderByAnalysedAtDesc(
            FinancialProfile financialProfile,
            com.finsight.backend.enums.AnalysisStatus status);

    /**
     * The latest analysis regardless of status.
     */
    Optional<FinancialAnalysis> findFirstByFinancialProfileOrderByAnalysedAtDesc(
            FinancialProfile financialProfile);

}