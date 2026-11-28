package com.finsight.backend.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.finsight.backend.dto.forecast.ForecastResponse;
import com.finsight.backend.entity.Transaction;

/**
 * HTTP client for POST /api/forecast (monthly expense forecast).
 */
@Component
public class AiForecastClient {

    private final RestClient restClient;

    /**
     * Reads the AI service address from `finsight.ai-service.url` (default
     * http://127.0.0.1:8000) and builds a RestClient for it.
     */
    public AiForecastClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

    /**
     * Send the transactions and the number of months to predict (at least 1) and
     * return the forecast.
     */
    public ForecastResponse forecast(List<Transaction> transactions, int monthsAhead) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("transactions", buildTransactionPayload(transactions));
        request.put("monthsAhead", Math.max(monthsAhead, 1));

        ForecastResponse response = restClient.post()
                .uri("/api/forecast")
                .contentType(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .accept(Objects.requireNonNull(MediaType.APPLICATION_JSON))
                .body(request)
                .retrieve()
                .body(ForecastResponse.class);

        if (response == null) {
            throw new IllegalStateException("AI forecasting service returned an empty response.");
        }

        return response;
    }

    /**
     * Convert Transaction entities to the AI JSON shape (duplicate of
     * AiTransactionPayloadBuilder.build()).
     */
    private List<Map<String, Object>> buildTransactionPayload(List<Transaction> transactions) {
        List<Map<String, Object>> payload = new ArrayList<>();

        if (transactions == null) {
            return payload;
        }

        for (Transaction transaction : transactions) {
            if (transaction == null) {
                continue;
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", transaction.getId());

            LocalDateTime transactionDate = transaction.getTransactionDate();
            item.put("transactionDate", transactionDate == null ? "" : transactionDate.toString());

            item.put("description", transaction.getDescription() == null ? "" : transaction.getDescription());

            BigDecimal amount = transaction.getAmount();
            item.put("amount", amount == null ? BigDecimal.ZERO : amount);

            item.put("type", transaction.getType() == null ? "" : transaction.getType().name());
            item.put("category", transaction.getCategory() == null ? "OTHER" : transaction.getCategory().name());
            item.put("paymentMethod", transaction.getPaymentMethod() == null ? "" : transaction.getPaymentMethod().name());

            payload.add(item);
        }

        return payload;
    }
}
