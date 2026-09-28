/**
 * FinSight File Notes: Sends recurring-subscription detection requests to the Python AI service.
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

import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.entity.Transaction;

@Component
public class AiSubscriptionClient {

    private final RestClient restClient;

    public AiSubscriptionClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

    public SubscriptionDetectionResponse detect(List<Transaction> transactions) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("transactions", AiTransactionPayloadBuilder.build(transactions));

        SubscriptionDetectionResponse response = restClient.post()
                .uri("/api/subscriptions")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .accept(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(request)
                .retrieve()
                .body(SubscriptionDetectionResponse.class);

        if (response == null) {
            throw new IllegalStateException("Python subscription service returned an empty response.");
        }
        return response;
    }
}
