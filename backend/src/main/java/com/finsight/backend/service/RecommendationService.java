/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service;

import com.finsight.backend.dto.recommendation.RecommendationResponse;

public interface RecommendationService {

    RecommendationResponse generateRecommendations(Long uploadHistoryId);

    RecommendationResponse getRecommendations(Long uploadHistoryId);
}
