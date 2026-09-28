/**
 * FinSight File Notes: Checks whether the Python AI service is reachable before a financial analysis starts.
 */
package com.finsight.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AiServiceHealthClient {

    private final RestClient restClient;
    private final String baseUrl;

    public AiServiceHealthClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.baseUrl = java.util.Objects.requireNonNull(
                AiServiceUrlResolver.resolve(aiServiceUrl),
                "FinSight AI service URL must not be null."
        );
        this.restClient = AiRestClientFactory.create(restClientBuilder, this.baseUrl);
    }

    public void requireAvailable() {
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .toEntity(String.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("FinSight AI service is unavailable at " + baseUrl + ".");
            }
        } catch (RestClientException exception) {
            throw new IllegalStateException(
                    "FinSight AI service could not be reached at " + baseUrl
                            + ". Start the Python service with: python -m uvicorn app.main:app --reload --port 8000",
                    exception
            );
        }
    }
}
