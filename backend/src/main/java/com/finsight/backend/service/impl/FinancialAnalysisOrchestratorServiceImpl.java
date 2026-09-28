/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.dto.alert.AlertResponse;
import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.orchestrator.FinancialAnalysisOrchestratorResponse;
import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.enums.AnalysisStatus;
import com.finsight.backend.client.AiServiceHealthClient;
import com.finsight.backend.service.AlertService;
import com.finsight.backend.service.FinancialAnalysisOrchestratorService;
import com.finsight.backend.service.RecommendationService;
import com.finsight.backend.service.SubscriptionDetectionService;
import com.finsight.backend.service.TransactionClassificationService;
import com.finsight.backend.service.TransactionAnalysisService;
import com.finsight.backend.service.UnusualSpendingDetectionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class FinancialAnalysisOrchestratorServiceImpl
        implements FinancialAnalysisOrchestratorService {

    private final TransactionClassificationService
            transactionClassificationService;

    private final TransactionAnalysisService
            transactionAnalysisService;

    private final SubscriptionDetectionService
            subscriptionDetectionService;

    private final UnusualSpendingDetectionService
            unusualSpendingDetectionService;

    private final RecommendationService
            recommendationService;

    private final AlertService
            alertService;

    private final AiServiceHealthClient
            aiServiceHealthClient;


    /*
     * ============================================================
     * RUN COMPLETE FINANCIAL ANALYSIS
     * ============================================================
     *
     * Execution order:
     *
     * 1. Transaction Classification
     * 2. Transaction Analysis
     * 3. Subscription Detection
     * 4. Unusual Spending Detection
     * 5. Recommendation Generation
     * 6. Alert Generation
     * 7. Retrieve latest synchronized transaction analysis
     * 8. Build final orchestrator response
     *
     * ============================================================
     */

    @Override
    @Transactional
    public FinancialAnalysisOrchestratorResponse
            runFinancialAnalysis(
                    Long uploadHistoryId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );

        // Fail fast with a clear local-service message instead of partially
        // processing an upload when the Python AI service is unavailable.
        aiServiceHealthClient.requireAvailable();

        /*
         * --------------------------------------------------------
         * STEP 1
         * Transaction Classification
         * --------------------------------------------------------
         *
         * Classification runs first so all downstream analysis
         * agents use the updated Transaction.category values.
         */

        transactionClassificationService
                .classifyUpload(uploadHistoryId);


        /*
         * --------------------------------------------------------
         * STEP 2
         * Transaction Analysis
         * --------------------------------------------------------
         */

        TransactionAnalysisResponse
                initialTransactionAnalysis =
                transactionAnalysisService
                        .analyseUpload(
                                uploadHistoryId
                        );


        if (initialTransactionAnalysis == null) {

            throw new IllegalStateException(
                    "Transaction analysis returned null."
            );
        }


        /*
         * --------------------------------------------------------
         * STEP 3
         * Subscription Detection
         * --------------------------------------------------------
         */

        SubscriptionDetectionResponse
                subscriptionDetection =
                subscriptionDetectionService
                        .detectSubscriptions();


        if (subscriptionDetection == null) {

            subscriptionDetection =
                    new SubscriptionDetectionResponse();

            subscriptionDetection
                    .setSubscriptionCount(0);
        }


        /*
         * --------------------------------------------------------
         * STEP 4
         * Unusual Spending Detection
         * --------------------------------------------------------
         */

        AnomalyDetectionResponse
                unusualSpendingDetection =
                unusualSpendingDetectionService
                        .detectAnomalies(
                                uploadHistoryId
                        );


        if (unusualSpendingDetection == null) {

            unusualSpendingDetection =
                    new AnomalyDetectionResponse();

            unusualSpendingDetection
                    .setAnomalyCount(0);
        }


        /*
         * --------------------------------------------------------
         * STEP 5
         * Recommendation Generation
         * --------------------------------------------------------
         *
         * This step also updates the financial_analysis record
         * with:
         *
         * - unusual transaction count
         * - subscription count
         * - recommendation count
         *
         * Therefore this must happen before retrieving the
         * final transaction analysis.
         */

        RecommendationResponse
                recommendations =
                recommendationService
                        .generateRecommendations(
                                uploadHistoryId
                        );


        if (recommendations == null) {

            recommendations =
                    new RecommendationResponse();

            recommendations
                    .setRecommendationCount(0);

            recommendations
                    .setUnusualTransactionCount(0);
        }


        /*
         * --------------------------------------------------------
         * STEP 6
         * Alert Generation
         * --------------------------------------------------------
         *
         * The alert layer also updates the financial_analysis
         * record.
         */

        AlertResponse alerts =
                alertService.generateAlerts(
                        uploadHistoryId
                );


        if (alerts == null) {

            alerts =
                    new AlertResponse();
        }


        /*
         * --------------------------------------------------------
         * STEP 7
         * Retrieve the latest synchronized analysis
         * --------------------------------------------------------
         *
         * IMPORTANT:
         *
         * Do NOT continue using initialTransactionAnalysis here.
         *
         * The initial response may contain:
         *
         * recurringSubscriptions = 8
         * unusualTransactions = 1
         * recommendationCount = 0
         *
         * After the other layers execute, the database has the
         * correct values:
         *
         * recurringSubscriptions = 2
         * unusualTransactions = 2
         * recommendationCount = 3
         *
         * Therefore retrieve the latest analysis again.
         */

        TransactionAnalysisResponse
                finalTransactionAnalysis =
                transactionAnalysisService
                        .getLatestAnalysis();


        if (finalTransactionAnalysis == null) {

            finalTransactionAnalysis =
                    initialTransactionAnalysis;
        }


        /*
         * --------------------------------------------------------
         * STEP 8
         * Synchronize specialized layer values
         * --------------------------------------------------------
         *
         * This provides an additional safety mechanism.
         *
         * Even if a stale response is returned by
         * getLatestAnalysis(), the orchestrator uses the actual
         * specialized layer outputs as the source of truth.
         */

        synchronizeTransactionAnalysis(
                finalTransactionAnalysis,
                subscriptionDetection,
                unusualSpendingDetection,
                recommendations
        );


        /*
         * --------------------------------------------------------
         * Build final response
         * --------------------------------------------------------
         */

        FinancialAnalysisOrchestratorResponse response =
                new FinancialAnalysisOrchestratorResponse();


        response.setAnalysisId(
                finalTransactionAnalysis.getAnalysisId()
        );


        response.setAnalysisJobId(
                finalTransactionAnalysis.getAnalysisJobId()
        );


        response.setUploadHistoryId(
                finalTransactionAnalysis
                        .getUploadHistoryId()
        );


        response.setFileName(
                finalTransactionAnalysis.getFileName()
        );


        response.setStatus(
                determineStatus(
                        finalTransactionAnalysis
                )
        );


        response.setTransactionAnalysis(
                finalTransactionAnalysis
        );


        response.setSubscriptionDetection(
                subscriptionDetection
        );


        response.setUnusualSpendingDetection(
                unusualSpendingDetection
        );


        response.setRecommendations(
                recommendations
        );


        response.setAlerts(
                alerts
        );


        response.setSummary(
                generateSummary(
                        finalTransactionAnalysis,
                        subscriptionDetection,
                        unusualSpendingDetection,
                        recommendations,
                        alerts
                )
        );


        return response;
    }


    /*
     * ============================================================
     * SYNCHRONIZE TRANSACTION ANALYSIS
     * ============================================================
     */

    private void synchronizeTransactionAnalysis(
            TransactionAnalysisResponse transactionAnalysis,
            SubscriptionDetectionResponse subscriptionDetection,
            AnomalyDetectionResponse unusualSpendingDetection,
            RecommendationResponse recommendations) {

        if (transactionAnalysis == null) {
            return;
        }


        /*
         * --------------------------------------------------------
         * Subscription count
         * --------------------------------------------------------
         */

        int subscriptionCount =
                0;


        if (subscriptionDetection != null) {

            subscriptionCount =
                    Math.max(
                            subscriptionDetection
                                    .getSubscriptionCount(),
                            0
                    );
        }


        transactionAnalysis
                .setRecurringSubscriptions(
                        subscriptionCount
                );


        /*
         * --------------------------------------------------------
         * Unusual transaction count
         * --------------------------------------------------------
         */

        int anomalyCount =
                0;


        if (unusualSpendingDetection != null) {

            anomalyCount =
                    Math.max(
                            unusualSpendingDetection
                                    .getAnomalyCount(),
                            0
                    );
        }


        transactionAnalysis
                .setUnusualTransactions(
                        anomalyCount
                );


        /*
         * --------------------------------------------------------
         * Recommendation count
         * --------------------------------------------------------
         */

        int recommendationCount =
                0;


        if (recommendations != null) {

            recommendationCount =
                    Math.max(
                            recommendations
                                    .getRecommendationCount(),
                            0
                    );
        }


        transactionAnalysis
                .setRecommendationCount(
                        recommendationCount
                );
    }


    /*
     * ============================================================
     * STATUS
     * ============================================================
     */

    private String determineStatus(
            TransactionAnalysisResponse transactionAnalysis) {

        if (transactionAnalysis == null) {
            return AnalysisStatus.COMPLETED.name();
        }


        if (transactionAnalysis.getStatus() == null) {
            return AnalysisStatus.COMPLETED.name();
        }


        return transactionAnalysis
                .getStatus()
                .name();
    }


    /*
     * ============================================================
     * SUMMARY
     * ============================================================
     */

    private String generateSummary(
            TransactionAnalysisResponse transactionAnalysis,
            SubscriptionDetectionResponse subscriptionDetection,
            AnomalyDetectionResponse unusualSpendingDetection,
            RecommendationResponse recommendations,
            AlertResponse alerts) {

        int totalTransactions =
                0;


        if (transactionAnalysis != null
                && transactionAnalysis
                        .getTotalTransactions() != null) {

            totalTransactions =
                    Math.max(
                            transactionAnalysis
                                    .getTotalTransactions(),
                            0
                    );
        }


        int subscriptionCount =
                0;


        if (subscriptionDetection != null) {

            subscriptionCount =
                    Math.max(
                            subscriptionDetection
                                    .getSubscriptionCount(),
                            0
                    );
        }


        int anomalyCount =
                0;


        if (unusualSpendingDetection != null) {

            anomalyCount =
                    Math.max(
                            unusualSpendingDetection
                                    .getAnomalyCount(),
                            0
                    );
        }


        int recommendationCount =
                0;


        if (recommendations != null) {

            recommendationCount =
                    Math.max(
                            recommendations
                                    .getRecommendationCount(),
                            0
                    );
        }


        int alertCount =
                0;


        if (alerts != null) {

            alertCount =
                    Math.max(
                            alerts.getAlertCount(),
                            0
                    );
        }


        return "Financial analysis completed successfully. "
                + totalTransactions
                + " transactions were analysed. "
                + subscriptionCount
                + " recurring subscriptions were detected. "
                + anomalyCount
                + " unusual transactions were detected. "
                + recommendationCount
                + " recommendations were generated. "
                + alertCount
                + " financial alerts were generated.";
    }
}