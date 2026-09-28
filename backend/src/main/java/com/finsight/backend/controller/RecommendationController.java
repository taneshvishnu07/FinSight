/**
 * FinSight File Notes: Exposes REST API endpoints for recommendation operations used by the FinSight frontend and services.
 */
package com.finsight.backend.controller;

import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.service.RecommendationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;


    @GetMapping("/{uploadHistoryId}")
    public ResponseEntity<RecommendationResponse>
            getRecommendations(
                    @PathVariable Long uploadHistoryId) {

        return ResponseEntity.ok(
                recommendationService.getRecommendations(uploadHistoryId)
        );
    }

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