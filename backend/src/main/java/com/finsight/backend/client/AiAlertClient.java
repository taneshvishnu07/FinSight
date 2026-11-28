package com.finsight.backend.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.finsight.backend.dto.alert.AlertResponse;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.FinancialProfile;

/**
 * HTTP client for the Python AI service endpoint POST /api/alerts (rule-based alert
 * agent). One Ai*Client exists per AI endpoint; they all follow the same pattern:
 * build a JSON request, POST it, fail if the body is empty.
 */
@Component
public class AiAlertClient {

    private final RestClient restClient;

    /**
     * Reads the AI service address from `finsight.ai-service.url` (default
     * http://127.0.0.1:8000) and builds a RestClient for it.
     */
    public AiAlertClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

    /**
     * Ask the AI service for alerts. Sends the transactions, the financial profile and
     * the subscription / unusual-spending results already computed (empty objects are
     * sent when those are null).
     */
    public AlertResponse generate(
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

        AlertResponse response = restClient.post()
                .uri("/api/alerts")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .accept(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(request)
                .retrieve()
                .body(AlertResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Python alert service returned an empty response."
            );
        }

        return response;
    }
}
