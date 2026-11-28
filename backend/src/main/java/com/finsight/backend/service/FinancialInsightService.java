package com.finsight.backend.service;

import com.finsight.backend.dto.insight.FinancialInsightResponse;

/**
 * Builds the financial health score and prioritised insights shown on the dashboard
 * and report. Implementation: FinancialInsightServiceImpl.
 */
public interface FinancialInsightService {

    /**
     * Calculate insights for the logged-in user from their saved data.
     */
    FinancialInsightResponse generateInsights();

}