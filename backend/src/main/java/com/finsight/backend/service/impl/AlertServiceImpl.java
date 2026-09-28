/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.client.AiAlertClient;
import com.finsight.backend.dto.alert.AlertResponse;
import com.finsight.backend.dto.anomaly.AnomalyDetectionResponse;
import com.finsight.backend.dto.subscription.SubscriptionDetectionResponse;
import com.finsight.backend.entity.AnalysisJob;
import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.SpendingAlert;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.AlertSeverity;
import com.finsight.backend.enums.AlertType;
import com.finsight.backend.enums.AnalysisStatus;
import com.finsight.backend.repository.AnalysisJobRepository;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.SpendingAlertRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.AlertService;
import com.finsight.backend.service.SubscriptionDetectionService;
import com.finsight.backend.service.UnusualSpendingDetectionService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class AlertServiceImpl implements AlertService {

    private static final String PYTHON_GENERATED_BY =
            "PYTHON_RULE_BASED_ALERT_AGENT";

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final FinancialProfileRepository financialProfileRepository;
    private final UploadHistoryRepository uploadHistoryRepository;
    private final AnalysisJobRepository analysisJobRepository;
    private final SpendingAlertRepository spendingAlertRepository;
    private final SubscriptionDetectionService subscriptionDetectionService;
    private final UnusualSpendingDetectionService unusualSpendingDetectionService;
    private final AiAlertClient aiAlertClient;

    @Override
    public AlertResponse generateAlerts(Long uploadHistoryId) {
        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);
        UploadHistory uploadHistory = getUploadHistory(
                uploadHistoryId,
                profile
        );
        FinancialAnalysis analysis = getFinancialAnalysis(uploadHistory);

        List<Transaction> transactions =
                transactionRepository
                        .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                                uploadHistory,
                                user
                        );

        if (transactions == null) {
            transactions = new ArrayList<>();
        }

        SubscriptionDetectionResponse subscriptionAnalysis =
                subscriptionDetectionService.detectSubscriptions();

        AnomalyDetectionResponse anomalyAnalysis =
                unusualSpendingDetectionService.detectAnomalies(
                        uploadHistoryId
                );

        AlertResponse aiResponse = aiAlertClient.generate(
                transactions,
                profile,
                subscriptionAnalysis,
                anomalyAnalysis
        );

        if (aiResponse == null) {
            throw new IllegalStateException(
                    "Python alert service returned an empty response."
            );
        }

        List<SpendingAlert> existing =
                spendingAlertRepository.findByFinancialAnalysis(analysis);

        if (existing != null && !existing.isEmpty()) {
            spendingAlertRepository.deleteAll(existing);
        }

        List<SpendingAlert> savedAlerts = new ArrayList<>();

        List<AlertResponse.AlertItemResponse> items =
                aiResponse.getAlerts();

        if (items == null) {
            items = new ArrayList<>();
        }

        for (AlertResponse.AlertItemResponse item : items) {

            if (item == null || "LARGE_TRANSACTION".equalsIgnoreCase(item.getType())) {
                continue;
            }

            SpendingAlert alert = new SpendingAlert();

            alert.setFinancialAnalysis(analysis);

            alert.setType(
                    parseAlertType(item.getType())
            );

            alert.setSeverity(
                    parseSeverity(item.getSeverity())
            );

            alert.setTitle(
                    safeText(
                            item.getTitle(),
                            "Financial alert"
                    )
            );

            alert.setMessage(
                    safeText(
                            item.getMessage(),
                            "Review your recent financial activity."
                    )
            );

            alert.setGeneratedBy(
                    safeText(
                            item.getGeneratedBy(),
                            PYTHON_GENERATED_BY
                    )
            );

            alert.setIsRead(Boolean.FALSE);

            spendingAlertRepository.save(alert);
            savedAlerts.add(alert);
        }

        analysis.setUnusualTransactions(
                anomalyAnalysis == null
                        ? 0
                        : anomalyAnalysis.getAnomalyCount()
        );

        analysis.setRecurringSubscriptions(
                subscriptionAnalysis == null
                        ? 0
                        : subscriptionAnalysis.getSubscriptionCount()
        );

        analysis.setStatus(AnalysisStatus.COMPLETED);

        String summary = safeText(
                aiResponse.getSummary(),
                "Your financial alerts have been generated."
        );

        analysis.setSummary(summary);

        AlertResponse response = buildResponse(
                analysis,
                uploadHistory,
                savedAlerts
        );

        response.setSummary(summary);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public AlertResponse getAlerts(Long uploadHistoryId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);

        UploadHistory uploadHistory = getUploadHistory(
                uploadHistoryId,
                profile
        );

        FinancialAnalysis analysis =
                getFinancialAnalysis(uploadHistory);

        List<SpendingAlert> alerts =
                spendingAlertRepository.findByFinancialAnalysis(
                        analysis
                );

        if (alerts == null) {
            alerts = new ArrayList<>();
        }

        /*
         * Explicit lambda is used instead of SpendingAlert::getId
         * to avoid IDE null-safety warnings caused by the inherited
         * @NonNull entity ID contract.
         */
        alerts.sort(
                Comparator.comparing(
                        alert -> alert == null ? null : alert.getId(),
                        Comparator.nullsLast(
                                Comparator.reverseOrder()
                        )
                )
        );

        return buildResponse(
                analysis,
                uploadHistory,
                alerts
        );
    }

    @Override
    public AlertResponse markAllAsRead(Long uploadHistoryId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);

        UploadHistory uploadHistory = getUploadHistory(
                uploadHistoryId,
                profile
        );

        FinancialAnalysis analysis =
                getFinancialAnalysis(uploadHistory);

        List<SpendingAlert> alerts =
                spendingAlertRepository.findByFinancialAnalysis(
                        analysis
                );

        if (alerts == null) {
            alerts = new ArrayList<>();
        }

        for (SpendingAlert alert : alerts) {

            if (alert == null || alert.getType() == AlertType.LARGE_TRANSACTION) {
                continue;
            }

            alert.setIsRead(Boolean.TRUE);
            spendingAlertRepository.save(alert);
        }

        return buildResponse(
                analysis,
                uploadHistory,
                alerts
        );
    }

    @Override
    public AlertResponse markAsRead(Long uploadHistoryId, Long alertId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );
        Objects.requireNonNull(
                alertId,
                "Alert ID must not be null."
        );

        User user = getAuthenticatedUser();
        FinancialProfile profile = getFinancialProfile(user);
        UploadHistory uploadHistory = getUploadHistory(
                uploadHistoryId,
                profile
        );
        FinancialAnalysis analysis = getFinancialAnalysis(uploadHistory);

        SpendingAlert alert = spendingAlertRepository
                .findByIdAndFinancialAnalysis(alertId, analysis)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Alert not found for this upload history."
                ));

        alert.setIsRead(Boolean.TRUE);
        spendingAlertRepository.save(alert);

        List<SpendingAlert> alerts =
                spendingAlertRepository.findByFinancialAnalysis(analysis);

        if (alerts == null) {
            alerts = new ArrayList<>();
        }

        return buildResponse(
                analysis,
                uploadHistory,
                alerts
        );
    }

    private AlertResponse buildResponse(
            FinancialAnalysis analysis,
            UploadHistory uploadHistory,
            List<SpendingAlert> alerts) {

        AlertResponse response = new AlertResponse();

        response.setAnalysisId(
                analysis.getId()
        );

        response.setUploadHistoryId(
                uploadHistory.getId()
        );

        response.setGeneratedAt(
                LocalDateTime.now()
        );

        List<AlertResponse.AlertItemResponse> items =
                new ArrayList<>();

        int unreadCount = 0;

        if (alerts == null) {
            alerts = new ArrayList<>();
        }

        for (SpendingAlert alert : alerts) {

            if (alert == null || alert.getType() == AlertType.LARGE_TRANSACTION) {
                continue;
            }

            AlertResponse.AlertItemResponse item =
                    new AlertResponse.AlertItemResponse();

            item.setId(
                    alert.getId()
            );

            item.setType(
                    alert.getType() == null
                            ? null
                            : alert.getType().name()
            );

            item.setSeverity(
                    alert.getSeverity() == null
                            ? null
                            : alert.getSeverity().name()
            );

            item.setTitle(
                    safeText(
                            alert.getTitle(),
                            "Financial alert"
                    )
            );

            item.setMessage(
                    safeText(
                            alert.getMessage(),
                            "Review your recent financial activity."
                    )
            );

            item.setIsRead(
                    alert.getIsRead()
            );

            item.setGeneratedBy(
                    safeText(
                            alert.getGeneratedBy(),
                            PYTHON_GENERATED_BY
                    )
            );

            items.add(item);

            if (!Boolean.TRUE.equals(alert.getIsRead())) {
                unreadCount++;
            }
        }

        response.setAlerts(items);
        response.setAlertCount(items.size());
        response.setUnreadCount(unreadCount);

        response.setSummary(
                "We found "
                        + items.size()
                        + " alert(s) that may be useful when reviewing your finances."
        );

        return response;
    }

    private FinancialAnalysis getFinancialAnalysis(
            UploadHistory uploadHistory) {

        List<AnalysisJob> jobs =
                analysisJobRepository
                        .findByUploadHistoryOrderByStartedAtDesc(
                                uploadHistory
                        );

        if (jobs == null || jobs.isEmpty()) {
            throw new IllegalStateException(
                    "No analysis job exists for upload history "
                            + uploadHistory.getId()
            );
        }

        for (AnalysisJob job : jobs) {

            if (job == null) {
                continue;
            }

            FinancialAnalysis analysis =
                    job.getFinancialAnalysis();

            if (analysis != null) {
                return analysis;
            }
        }

        throw new IllegalStateException(
                "No financial analysis exists for upload history "
                        + uploadHistory.getId()
        );
    }

    private UploadHistory getUploadHistory(
            Long uploadHistoryId,
            FinancialProfile profile) {

        return uploadHistoryRepository
                .findByIdAndFinancialProfile(
                        uploadHistoryId,
                        profile
                )
                .orElseThrow(
                        () -> new IllegalArgumentException(
                                "Upload history not found."
                        )
                );
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

    private AlertType parseAlertType(String value) {

        if (value == null || value.isBlank()) {
            return AlertType.REMINDER;
        }

        try {
            return AlertType.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {
            return AlertType.REMINDER;
        }
    }

    private AlertSeverity parseSeverity(String value) {

        if (value == null || value.isBlank()) {
            return AlertSeverity.MEDIUM;
        }

        try {
            return AlertSeverity.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException exception) {
            return AlertSeverity.MEDIUM;
        }
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