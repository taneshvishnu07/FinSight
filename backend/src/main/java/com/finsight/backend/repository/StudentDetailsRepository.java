/**
 * FinSight File Notes: Provides database query and persistence operations for one FinSight entity.
 */
package com.finsight.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.StudentDetails;

public interface StudentDetailsRepository extends JpaRepository<StudentDetails, Long> {

    Optional<StudentDetails> findByFinancialProfile(FinancialProfile financialProfile);

}