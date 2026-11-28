package com.finsight.backend.controller;

import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.service.RecommendationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Recommendations for an upload, mounted under /api/recommendations.
 */
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;


    /**
     * GET /api/recommendations/{uploadHistoryId} - read the saved recommendations.
     */
    @GetMapping("/{uploadHistoryId}")
    public ResponseEntity<RecommendationResponse>
            getRecommendations(
                    @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                recommendationService.getRecommendations(uploadHistoryId)
        );
    }

    /**
     * POST /api/recommendations/generate/{uploadHistoryId} - ask the AI service for
     * fresh recommendations and save them.
     */
    @PostMapping("/generate/{uploadHistoryId}")
    public ResponseEntity<RecommendationResponse>
            generateRecommendations(
                    @PathVariable Long uploadHistoryId) {

        RecommendationResponse response =
                recommendationService
                        .generateRecommendations(
                                uploadHistoryId
                        );

        return ResponseEntity.ok(
                response
        );
    }
}