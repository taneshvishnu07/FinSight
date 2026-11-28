package com.finsight.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.Recommendation;

/**
 * Data access for Recommendation; list the recommendations of one analysis.
 */
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByFinancialAnalysis(
            FinancialAnalysis financialAnalysis);

}