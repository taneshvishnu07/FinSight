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
        RestClientException lastException = null;

        // The AI container may still be starting when the user immediately begins
        // an analysis. Retry briefly before reporting a deployment/network failure.
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ResponseEntity<String> response = restClient.get()
                        .uri("/health")
                        .retrieve()
                        .toEntity(String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    return;
                }

                throw new IllegalStateException(
                        "FinSight AI service returned HTTP " + response.getStatusCode().value()
                                + " from " + baseUrl + "."
                );
            } catch (RestClientException exception) {
                lastException = exception;
                if (attempt < 3) {
                    try {
                        Thread.sleep(1000L);
                    } catch (InterruptedException interruptedException) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(
                                "The FinSight AI service health check was interrupted.",
                                interruptedException
                        );
                    }
                }
            }
        }

        throw new IllegalStateException(
                "FinSight AI service could not be reached at " + baseUrl
                        + ". Check that the Railway AI service is deployed and listening on port 8000.",
                lastException
        );
    }
}
