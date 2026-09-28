/**
 * FinSight File Notes: Sends expense-forecasting requests to the Python AI service.
 */
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

@Component
public class AiForecastClient {

    private final RestClient restClient;

    public AiForecastClient(
            RestClient.Builder restClientBuilder,
            @Value("${finsight.ai-service.url:http://127.0.0.1:8000}") String aiServiceUrl) {
        this.restClient = AiRestClientFactory.create(restClientBuilder, aiServiceUrl);
    }

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
