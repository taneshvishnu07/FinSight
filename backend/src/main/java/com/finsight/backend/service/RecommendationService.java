package com.finsight.backend.service;

import com.finsight.backend.dto.recommendation.RecommendationResponse;

public interface RecommendationService {

    RecommendationResponse generateRecommendations(Long uploadHistoryId);

    RecommendationResponse getRecommendations(Long uploadHistoryId);
}
