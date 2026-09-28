/**
 * FinSight File Notes: Sends unusual-spending analysis requests to the Python AI service.
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
import com.finsight.backend.entity.Transaction;

@Component
public class AiAnomalyClient {

    private final RestClient restClient;

    public AiAnomalyClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

    public AnomalyDetectionResponse detect(List<Transaction> transactions) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("transactions", AiTransactionPayloadBuilder.build(transactions));

        AnomalyDetectionResponse response = restClient.post()
                .uri("/api/unusual-spending")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .accept(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(request)
                .retrieve()
                .body(AnomalyDetectionResponse.class);

        if (response == null) {
            throw new IllegalStateException("Python unusual spending service returned an empty response.");
        }
        return response;
    }
}
