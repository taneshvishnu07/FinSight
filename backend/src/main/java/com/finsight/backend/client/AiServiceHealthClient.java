package com.finsight.backend.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Checks that the Python AI service is running (GET /health) before an analysis
 * starts, so the user gets a clear error instead of a low-level connection failure.
 */
@Component
public class AiServiceHealthClient {

    private final RestClient restClient;
    private final String baseUrl;

    /**
     * Reads the AI service address from `finsight.ai-service.url` (default
     * http://127.0.0.1:8000) and builds a RestClient for it.
     */
    public AiServiceHealthClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.baseUrl = java.util.Objects.requireNonNull(
                AiServiceUrlResolver.resolve(aiServiceUrl),
                "FinSight AI service URL must not be null."
        );
        this.restClient = AiRestClientFactory.create(restClientBuilder, this.baseUrl);
    }

    /**
     * Throws IllegalStateException (with the command to start the service) if the AI
     * service is not reachable or does not answer 2xx.
     */
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
