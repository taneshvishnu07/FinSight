package com.finsight.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.finsight.backend.entity.EmployeeDetails;
import com.finsight.backend.entity.FinancialProfile;

/**
 * Data access for EmployeeDetails; look up by owning FinancialProfile.
 */
public interface EmployeeDetailsRepository extends JpaRepository<EmployeeDetails, Long> {

    Optional<EmployeeDetails> findByFinancialProfile(FinancialProfile financialProfile);

}