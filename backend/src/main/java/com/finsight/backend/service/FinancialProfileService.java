/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service;

import com.finsight.backend.dto.financialprofile.FinancialProfileCreateRequest;
import com.finsight.backend.dto.financialprofile.FinancialProfileResponse;

public interface FinancialProfileService {

    FinancialProfileResponse createProfile(
            FinancialProfileCreateRequest request
    );

    FinancialProfileResponse getMyProfile();

    FinancialProfileResponse updateProfile(
            FinancialProfileCreateRequest request
    );

    void deleteProfile();
}