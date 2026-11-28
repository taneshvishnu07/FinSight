package com.finsight.backend.service;

import com.finsight.backend.dto.recommendation.RecommendationResponse;

/**
 * Generates, stores and reads personalised recommendations. Implementation:
 * RecommendationServiceImpl.
 */
public interface RecommendationService {

    /**
     * Create new recommendations with the AI service and save them for the upload's
     * analysis.
     */
    RecommendationResponse generateRecommendations(Long uploadHistoryId);

    /**
     * Read the saved recommendations for the upload.
     */
    RecommendationResponse getRecommendations(Long uploadHistoryId);
}
