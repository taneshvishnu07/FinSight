package com.finsight.backend.dto.orchestrator;

import com.finsight.backend.dto.alert.AlertResponse;
import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Combined result of the full analysis pipeline: the basic analysis plus the
 * subscription, unusual-spending, recommendation and alert results and an overall
 * summary.
 */
@Getter
@Setter
@NoArgsConstructor
public class FinancialAnalysisOrchestratorResponse {

    private Long analysisId;

    private Long analysisJobId;

    private Long uploadHistoryId;

    private String fileName;

    private String status;

    private TransactionAnalysisResponse transactionAnalysis;

    private SubscriptionDetectionResponse subscriptionDetection;

    private AnomalyDetectionResponse unusualSpendingDetection;

    private RecommendationResponse recommendations;

    private AlertResponse alerts;

    private String summary;
}