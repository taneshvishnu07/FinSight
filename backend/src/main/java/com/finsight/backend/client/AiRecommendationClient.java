/**
 * FinSight File Notes: Sends recommendation-generation requests to the Python AI service.
 */
package com.finsight.backend.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.FinancialProfile;

@Component
public class AiRecommendationClient {

    private final RestClient restClient;

    public AiRecommendationClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

    public RecommendationResponse generate(
            List<Transaction> transactions,
            FinancialProfile financialProfile,
            SubscriptionDetectionResponse subscriptionAnalysis,
            AnomalyDetectionResponse anomalyAnalysis) {

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("transactions", AiTransactionPayloadBuilder.build(transactions));
        request.put("financialProfile", AiFinancialProfilePayloadBuilder.build(financialProfile));
        request.put("subscriptionAnalysis", Objects.requireNonNullElse(
                subscriptionAnalysis,
                new SubscriptionDetectionResponse()
        ));
        request.put("anomalyAnalysis", Objects.requireNonNullElse(
                anomalyAnalysis,
                new AnomalyDetectionResponse()
        ));

        RecommendationResponse response = restClient.post()
                .uri("/api/recommendations")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .accept(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(request)
                .retrieve()
                .body(RecommendationResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Python recommendation service returned an empty response."
            );
        }

        return response;
    }
}
