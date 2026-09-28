/**
 * FinSight File Notes: Defines the data structure used to send requests to or return responses from the FinSight backend API.
 */
package com.finsight.backend.dto.orchestrator;

import com.finsight.backend.dto.alert.AlertResponse;
import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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