package com.finsight.backend.client;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.finsight.backend.dto.classification.ClassificationResponse;
import com.finsight.backend.entity.Transaction;

/**
 * HTTP client for POST /api/classification (assigns a category to each transaction
 * description).
 */
@Component
public class AiClassificationClient {

    private final RestClient restClient;

    /**
     * Reads the AI service address from `finsight.ai-service.url` (default
     * http://127.0.0.1:8000) and builds a RestClient for it.
     */
    public AiClassificationClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

    /**
     * Send the transactions and return the predicted category (and confidence) for
     * each.
     */
    public ClassificationResponse classify(List<Transaction> transactions) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("transactions", AiTransactionPayloadBuilder.build(transactions));

        ClassificationResponse response = restClient.post()
                .uri("/api/classification")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .accept(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(request)
                .retrieve()
                .body(ClassificationResponse.class);

        if (response == null) {
            throw new IllegalStateException(
                    "Python transaction classification service returned an empty response."
            );
        }

        return response;
    }
}
