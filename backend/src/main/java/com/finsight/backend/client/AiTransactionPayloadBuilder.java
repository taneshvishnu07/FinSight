/**
 * FinSight File Notes: Converts stored transactions into the JSON structure expected by the Python AI service.
 */
package com.finsight.backend.client;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.finsight.backend.entity.Transaction;

public final class AiTransactionPayloadBuilder {

    private AiTransactionPayloadBuilder() {
    }

    public static List<Map<String, Object>> build(List<Transaction> transactions) {
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
            LocalDateTime date = transaction.getTransactionDate();
            item.put("transactionDate", date == null ? "" : date.toString());
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
