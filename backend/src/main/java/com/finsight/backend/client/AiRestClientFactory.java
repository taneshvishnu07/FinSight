/**
 * FinSight File Notes: Creates the HTTP client used for reliable communication with the local or deployed AI service.
 */
package com.finsight.backend.client;

import java.util.Objects;

import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.lang.NonNull;
import org.springframework.web.client.RestClient;

/**
 * Creates RestClient instances for the local FinSight FastAPI service.
 *
 * FastAPI/Uvicorn is run as an HTTP/1.1 server in local development. The
 * standard Spring Boot RestClient builder may select an HTTP client that
 * negotiates HTTP/2. For the local Python service we deliberately use
 * SimpleClientHttpRequestFactory (HttpURLConnection) so the request is sent
 * as ordinary HTTP/1.1 and Uvicorn does not receive an HTTP/2/TLS preface.
 */
public final class AiRestClientFactory {

    private AiRestClientFactory() {
    }

    @NonNull
    public static RestClient create(RestClient.Builder builder, String configuredUrl) {
        RestClient.Builder safeBuilder = Objects.requireNonNull(
                builder,
                "RestClient.Builder must not be null."
        );
        String baseUrl = Objects.requireNonNull(
                AiServiceUrlResolver.resolve(configuredUrl),
                "FinSight AI service URL must not be null."
        );

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(5000);
        requestFactory.setReadTimeout(120000);

        return safeBuilder
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .build();
    }
}
