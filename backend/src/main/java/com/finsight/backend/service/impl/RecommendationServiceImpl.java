/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.client.AiRecommendationClient;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.recommendation.RecommendationItemResponse;
import com.finsight.backend.dto.recommendation.RecommendationResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.entity.AnalysisJob;
import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Recommendation;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.AnalysisStatus;
import com.finsight.backend.enums.RecommendationPriority;
import com.finsight.backend.enums.RecommendationType;
import com.finsight.backend.repository.AnalysisJobRepository;
import com.finsight.backend.repository.FinancialAnalysisRepository;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.RecommendationRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.RecommendationService;
import com.finsight.backend.service.SubscriptionDetectionService;
import com.finsight.backend.service.UnusualSpendingDetectionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class RecommendationServiceImpl
        implements RecommendationService {

    private static final String PYTHON_GENERATED_BY =
            "PYTHON_RULE_BASED_RECOMMENDATION_AGENT";

    private final TransactionRepository transactionRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final UserRepository userRepository;
    private final RecommendationRepository recommendationRepository;
    private final FinancialAnalysisRepository financialAnalysisRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final SubscriptionDetectionService subscriptionDetectionService;
    private final UnusualSpendingDetectionService unusualSpendingDetectionService;
    private final AiRecommendationClient aiRecommendationClient;

    @Override
    @Transactional(readOnly = true)
    public RecommendationResponse getRecommendations(
            Long uploadHistoryId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);

        UploadHistory uploadHistory =
                uploadHistoryRepository
                        .findByIdAndFinancialProfile(
                                uploadHistoryId,
                                profile
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Upload history not found."
                                ));

        List<AnalysisJob> jobs =
                analysisJobRepository
                        .findByUploadHistoryOrderByStartedAtDesc(
                                uploadHistory
                        );

        FinancialAnalysis analysis = null;
        if (jobs != null) {
            for (AnalysisJob job : jobs) {
                if (job != null && job.getFinancialAnalysis() != null) {
                    analysis = job.getFinancialAnalysis();
                    break;
                }
            }
        }

        if (analysis == null) {
            throw new IllegalArgumentException(
                    "No completed financial analysis exists for this upload."
            );
        }

        RecommendationResponse response =
                new RecommendationResponse();

        response.setUploadHistoryId(uploadHistory.getId());
        response.setFileName(
                safeText(uploadHistory.getOriginalFileName(),
                        "Uploaded transactions")
        );
        response.setTotalIncome(safeAmount(analysis.getTotalIncome()));
        response.setTotalExpense(safeAmount(analysis.getTotalExpense()));
        response.setTotalSavings(safeAmount(analysis.getTotalSavings()));
        response.setUnusualTransactionCount(
                analysis.getUnusualTransactions() == null
                        ? 0 : analysis.getUnusualTransactions()
        );
        response.setRecommendationCount(
                analysis.getRecommendationCount() == null
                        ? 0 : analysis.getRecommendationCount()
        );

        List<RecommendationItemResponse> items = new ArrayList<>();
        List<Recommendation> recommendations =
                recommendationRepository.findByFinancialAnalysis(analysis);

        if (recommendations != null) {
            for (Recommendation recommendation : recommendations) {
                if (recommendation != null) {
                    items.add(convertToResponse(recommendation));
                }
            }
        }

        response.setRecommendations(items);
        response.setRecommendationCount(items.size());
        response.setSummary(
                safeText(analysis.getSummary(),
                        "Your financial recommendations are ready.")
        );

        return response;
    }

    @Override
    public RecommendationResponse generateRecommendations(
            Long uploadHistoryId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );

        User user = getAuthenticatedUser();

        FinancialProfile financialProfile =
                getFinancialProfile(user);

        UploadHistory uploadHistory =
                uploadHistoryRepository
                        .findByIdAndFinancialProfile(
                                uploadHistoryId,
                                financialProfile
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "Upload history not found."
                                )
                        );

        List<Transaction> transactions =
                transactionRepository
                        .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                                uploadHistory,
                                user
                        );

        if (transactions == null || transactions.isEmpty()) {
            throw new IllegalArgumentException(
                    "No transactions were found for this upload."
            );
        }

        SubscriptionDetectionResponse subscriptionAnalysis =
                subscriptionDetectionService.detectSubscriptions();

        AnomalyDetectionResponse anomalyAnalysis =
                unusualSpendingDetectionService.detectAnomalies(
                        uploadHistoryId
                );

        RecommendationResponse aiResponse =
                aiRecommendationClient.generate(
                        transactions,
                        financialProfile,
                        subscriptionAnalysis,
                        anomalyAnalysis
                );

        if (aiResponse == null) {
            throw new IllegalStateException(
                    "Python recommendation service returned an empty response."
            );
        }

        FinancialAnalysis financialAnalysis =
                getOrCreateFinancialAnalysisForUpload(
                        uploadHistory,
                        financialProfile,
                        safeAmount(aiResponse.getTotalIncome()),
                        safeAmount(aiResponse.getTotalExpense()),
                        safeAmount(aiResponse.getTotalSavings())
                );

        List<Recommendation> existing =
                recommendationRepository
                        .findByFinancialAnalysis(
                                financialAnalysis
                        );

        if (existing != null && !existing.isEmpty()) {
            recommendationRepository.deleteAll(existing);
        }

        List<RecommendationItemResponse> items =
                aiResponse.getRecommendations();

        if (items == null) {
            items = new ArrayList<>();
        }

        List<Recommendation> saved =
                new ArrayList<>();

        for (RecommendationItemResponse item : items) {

            if (item == null) {
                continue;
            }
          
            Recommendation entity =
                    convertToEntity(
                            item,
                            financialAnalysis
                    );

            Recommendation nonNullEntity =
                    java.util.Objects.requireNonNull(
                            entity,
                            "Recommendation entity must not be null."
                    );

            saveRecommendation(nonNullEntity);
            saved.add(nonNullEntity);
        }

        int anomalyCount =
                anomalyAnalysis == null
                        ? 0
                        : anomalyAnalysis.getAnomalyCount();

        int subscriptionCount =
                subscriptionAnalysis == null
                        ? 0
                        : subscriptionAnalysis.getSubscriptionCount();

        financialAnalysis.setStatus(
                AnalysisStatus.COMPLETED
        );

        financialAnalysis.setTotalIncome(
                safeAmount(aiResponse.getTotalIncome())
        );

        financialAnalysis.setTotalExpense(
                safeAmount(aiResponse.getTotalExpense())
        );

        financialAnalysis.setTotalSavings(
                safeAmount(aiResponse.getTotalSavings())
        );

        financialAnalysis.setUnusualTransactions(
                anomalyCount
        );

        financialAnalysis.setRecommendationCount(
                saved.size()
        );

        financialAnalysis.setRecurringSubscriptions(
                subscriptionCount
        );

        financialAnalysis.setSummary(
                safeText(
                        aiResponse.getSummary(),
                        "Your financial recommendations have been generated."
                )
        );

        financialAnalysisRepository.save(
                financialAnalysis
        );

        List<RecommendationItemResponse> responseItems =
                new ArrayList<>();

        for (Recommendation recommendation : saved) {

            if (recommendation == null) {
                continue;
            }

            responseItems.add(
                    convertToResponse(recommendation)
            );
        }

        aiResponse.setUploadHistoryId(
                uploadHistory.getId()
        );

        aiResponse.setFileName(
                safeText(
                        uploadHistory.getOriginalFileName(),
                        "Uploaded transactions"
                )
        );

        aiResponse.setRecommendationCount(
                responseItems.size()
        );

        aiResponse.setUnusualTransactionCount(
                anomalyCount
        );

        aiResponse.setRecommendations(
                responseItems
        );

        aiResponse.setSummary(
                safeText(
                        aiResponse.getSummary(),
                        "Your financial recommendations have been generated."
                )
        );

        return aiResponse;
    }

    private void saveRecommendation(Recommendation recommendation) {
        Recommendation nonNullRecommendation = Objects.requireNonNull(
                recommendation,
                "Recommendation entity must not be null."
        );
        recommendationRepository.save(nonNullRecommendation);
    }

    private FinancialAnalysis getOrCreateFinancialAnalysisForUpload(
            UploadHistory uploadHistory,
            FinancialProfile financialProfile,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalSavings) {

        List<AnalysisJob> jobs =
                analysisJobRepository
                        .findByUploadHistoryOrderByStartedAtDesc(
                                uploadHistory
                        );

        /*
         * Explicit loop instead of:
         *
         * .filter(Objects::nonNull)
         * .map(job -> job.getFinancialAnalysis())
         * .filter(Objects::nonNull)
         *
         * This avoids Eclipse/IDE null-safety warnings involving
         * JPA entity methods and @NonNull contracts.
         */
        if (jobs != null) {

            for (AnalysisJob job : jobs) {

                if (job == null) {
                    continue;
                }

                FinancialAnalysis existing =
                        job.getFinancialAnalysis();

                if (existing == null) {
                    continue;
                }

                existing.setFinancialProfile(
                        financialProfile
                );

                existing.setTotalIncome(
                        totalIncome
                );

                existing.setTotalExpense(
                        totalExpense
                );

                existing.setTotalSavings(
                        totalSavings
                );

                return existing;
            }
        }

        FinancialAnalysis analysis =
                new FinancialAnalysis();

        analysis.setFinancialProfile(
                financialProfile
        );

        analysis.setStatus(
                AnalysisStatus.PROCESSING
        );

        analysis.setTotalIncome(
                totalIncome
        );

        analysis.setTotalExpense(
                totalExpense
        );

        analysis.setTotalSavings(
                totalSavings
        );

        analysis.setRecurringSubscriptions(0);
        analysis.setUnusualTransactions(0);
        analysis.setRecommendationCount(0);

        List<AnalysisJob> analysisJobs =
                analysisJobRepository
                        .findByUploadHistoryOrderByStartedAtDesc(
                                uploadHistory
                        );

        if (analysisJobs == null
                || analysisJobs.isEmpty()
                || analysisJobs.get(0) == null) {

            throw new IllegalStateException(
                    "No analysis job exists for upload history "
                            + uploadHistory.getId()
            );
        }

        AnalysisJob analysisJob =
                analysisJobs.get(0);

        analysis.setAnalysisJob(
                analysisJob
        );

        analysisJob.setFinancialAnalysis(
                analysis
        );

        return financialAnalysisRepository.save(
                analysis
        );
    }

    private Recommendation convertToEntity(
            RecommendationItemResponse item,
            FinancialAnalysis financialAnalysis) {

        Recommendation entity =
                new Recommendation();

        entity.setFinancialAnalysis(
                financialAnalysis
        );

        entity.setType(
                parseRecommendationType(
                        item.getType()
                )
        );

        entity.setPriority(
                parseRecommendationPriority(
                        item.getPriority()
                )
        );

        entity.setTitle(
                safeText(
                        item.getTitle(),
                        "Financial recommendation"
                )
        );

        entity.setDescription(
                safeText(
                        item.getMessage(),
                        "Take a quick look at your recent financial activity."
                )
        );

        entity.setExpectedSavings(
                safeAmount(
                        item.getRelatedAmount()
                )
        );

        entity.setRelatedCategory(
                item.getRelatedCategory()
        );

        entity.setRelatedMerchant(
                item.getRelatedMerchant()
        );

        entity.setGeneratedBy(
                safeText(
                        item.getGeneratedBy(),
                        PYTHON_GENERATED_BY
                )
        );

        entity.setAccepted(
                Boolean.FALSE
        );

        entity.setDismissed(
                Boolean.FALSE
        );

        return entity;
    }

    private RecommendationItemResponse convertToResponse(
            Recommendation entity) {

        RecommendationItemResponse response =
                new RecommendationItemResponse();

        response.setId(
                entity.getId()
        );

        response.setType(
                entity.getType() == null
                        ? "OTHER"
                        : entity.getType().name()
        );

        response.setPriority(
                entity.getPriority() == null
                        ? "LOW"
                        : entity.getPriority().name()
        );

        response.setTitle(
                safeText(
                        entity.getTitle(),
                        "Financial recommendation"
                )
        );

        response.setMessage(
                safeText(
                        entity.getDescription(),
                        "Take a quick look at your recent financial activity."
                )
        );

        response.setRelatedAmount(
                entity.getExpectedSavings()
        );

        response.setRelatedCategory(
                entity.getRelatedCategory()
        );

        response.setRelatedMerchant(
                entity.getRelatedMerchant()
        );

        response.setGeneratedBy(
                safeText(
                        entity.getGeneratedBy(),
                        PYTHON_GENERATED_BY
                )
        );

        return response;
    }

    private RecommendationType parseRecommendationType(
            String value) {

        if (value == null || value.isBlank()) {
            return RecommendationType.OTHER;
        }

        try {
            return RecommendationType.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {
            return RecommendationType.OTHER;
        }
    }

    private RecommendationPriority parseRecommendationPriority(
            String value) {

        if (value == null || value.isBlank()) {
            return RecommendationPriority.LOW;
        }

        try {
            return RecommendationPriority.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {
            return RecommendationPriority.LOW;
        }
    }

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication.getName() == null
                || "anonymousUser".equals(
                        authentication.getName()
                )) {

            throw new IllegalStateException(
                    "User is not authenticated."
            );
        }

        return userRepository
                .findByEmail(authentication.getName())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Authenticated user not found."
                        )
                );
    }

    private FinancialProfile getFinancialProfile(
            User user) {

        return financialProfileRepository
                .findByUser(user)
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Financial profile not found."
                        )
                );
    }

    private BigDecimal safeAmount(
            BigDecimal value) {

        if (value == null) {
            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return value.setScale(
                2,
                RoundingMode.HALF_UP
        );
    }

    private String safeText(
            String value,
            String fallback) {

        if (value == null || value.isBlank()) {
            return fallback;
        }

        return value.trim();
    }
}