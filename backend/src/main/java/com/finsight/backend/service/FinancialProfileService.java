package com.finsight.backend.service;

import com.finsight.backend.dto.financialprofile.FinancialProfileCreateRequest;
import com.finsight.backend.dto.financialprofile.FinancialProfileResponse;

/**
 * Create, read, update and delete the logged-in user's financial profile.
 * Implementation: FinancialProfileServiceImpl.
 */
public interface FinancialProfileService {

    /**
     * Create the profile (only one per user).
     */
    FinancialProfileResponse createProfile(
            FinancialProfileCreateRequest request
    );

    /**
     * Return the logged-in user's profile.
     */
    FinancialProfileResponse getMyProfile();

    /**
     * Replace the profile data, including the student/employee details.
     */
    FinancialProfileResponse updateProfile(
            FinancialProfileCreateRequest request
    );

    /**
     * Delete the profile and everything attached to it.
     */
    void deleteProfile();
}