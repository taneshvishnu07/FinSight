/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.dto.insight.FinancialInsightResponse;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.TransactionType;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.FinancialInsightService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialInsightServiceImpl
        implements FinancialInsightService {

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final FinancialProfileRepository financialProfileRepository;

    private static final BigDecimal ONE_HUNDRED =
            BigDecimal.valueOf(100);

    private static final BigDecimal HIGH_SPENDING_PERCENTAGE =
            BigDecimal.valueOf(30);

    private static final BigDecimal STRONG_SAVINGS_RATE =
            BigDecimal.valueOf(50);

    private static final BigDecimal MODERATE_SAVINGS_RATE =
            BigDecimal.valueOf(20);

    @Override
    public FinancialInsightResponse generateInsights() {

        User user = getAuthenticatedUser();

        FinancialProfile financialProfile =
                getFinancialProfile(user);

        List<Transaction> transactions =
                transactionRepository
                        .findByUserOrderByTransactionDateDesc(user);

        List<Transaction> validTransactions =
                filterTransactions(
                        transactions,
                        financialProfile
                );

        FinancialMetrics metrics =
                calculateMetrics(validTransactions);

        FinancialInsightResponse response =
                new FinancialInsightResponse();

        BigDecimal savingsRate =
                calculateSavingsRate(
                        metrics.totalIncome,
                        metrics.totalSavings
                );

        int healthScore =
                calculateFinancialHealthScore(
                        metrics,
                        savingsRate
                );

        response.setFinancialHealthScore(
                healthScore
        );

        response.setFinancialHealthStatus(
                determineHealthStatus(
                        healthScore
                )
        );

        response.setSavingsRate(
                savingsRate
        );

        response.setOverallSummary(
                buildOverallSummary(
                        metrics,
                        savingsRate,
                        healthScore
                )
        );

        List<FinancialInsightResponse.Insight> insights =
                new ArrayList<>();

        List<String> positiveBehaviours =
                new ArrayList<>();

        List<String> financialRisks =
                new ArrayList<>();

        List<String> priorityActions =
                new ArrayList<>();

        /*
         * --------------------------------------------------------
         * SAVINGS INSIGHT
         * --------------------------------------------------------
         */

        if (savingsRate.compareTo(
                STRONG_SAVINGS_RATE
        ) >= 0) {

            positiveBehaviours.add(
                    "You are maintaining a strong savings rate of "
                    + formatAmount(savingsRate)
                    + "%."
            );

            insights.add(
                    createInsight(
                            "POSITIVE_BEHAVIOUR",
                            "LOW",
                            "Strong savings behaviour",
                            "Your current savings rate is "
                                    + formatAmount(savingsRate)
                                    + "%, which indicates strong "
                                    + "capacity to retain income.",
                            metrics.totalSavings,
                            null,
                            null
                    )
            );

        } else if (savingsRate.compareTo(
                MODERATE_SAVINGS_RATE
        ) >= 0) {

            positiveBehaviours.add(
                    "You are maintaining a moderate savings rate of "
                    + formatAmount(savingsRate)
                    + "%."
            );

        } else if (metrics.totalIncome.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            financialRisks.add(
                    "Your savings rate is relatively low at "
                    + formatAmount(savingsRate)
                    + "%."
            );

            priorityActions.add(
                    "Review discretionary spending and "
                    + "increase the amount saved from monthly income."
            );
        }

        /*
         * --------------------------------------------------------
         * CATEGORY ANALYSIS
         * --------------------------------------------------------
         */

        List<CategorySpending> categorySpendings =
                calculateCategorySpending(
                        validTransactions,
                        metrics.totalExpense
                );

        for (CategorySpending categorySpending
                : categorySpendings) {

            if (categorySpending == null) {
                continue;
            }

            if (categorySpending.percentage.compareTo(
                    HIGH_SPENDING_PERCENTAGE
            ) >= 0) {

                String category =
                        categorySpending.category;

                BigDecimal amount =
                        categorySpending.amount;

                BigDecimal percentage =
                        categorySpending.percentage;

                financialRisks.add(
                        category
                                + " represents "
                                + formatAmount(percentage)
                                + "% of your total expenses."
                );

                priorityActions.add(
                        "Review spending in the "
                                + category
                                + " category and consider "
                                + "setting a spending limit."
                );

                insights.add(
                        createInsight(
                                "HIGH_CATEGORY_SPENDING",
                                "HIGH",
                                "High "
                                        + category
                                        + " spending",
                                "Spending on "
                                        + category
                                        + " accounts for "
                                        + formatAmount(percentage)
                                        + "% of your total expenses.",
                                amount,
                                category,
                                null
                        )
                );

                /*
                 * Only report the highest category as the primary
                 * category insight to avoid excessive duplication.
                 */

                break;
            }
        }

        /*
         * --------------------------------------------------------
         * LARGEST EXPENSE
         * --------------------------------------------------------
         */

        Transaction largestExpense =
                findLargestExpense(
                        validTransactions
                );

        if (largestExpense != null) {

            positiveBehaviours.add(
                    "Your largest individual expense was "
                            + formatCurrency(
                                    largestExpense.getAmount()
                            )
                            + " for "
                            + safeDescription(
                                    largestExpense
                            )
                            + "."
            );
        }

        /*
         * --------------------------------------------------------
         * SUBSCRIPTION INSIGHT
         * --------------------------------------------------------
         */

        Map<String, List<Transaction>> merchantGroups =
                groupByMerchant(
                        validTransactions
                );

        int subscriptionCount =
                countLikelySubscriptions(
                        merchantGroups
                );

        BigDecimal monthlySubscriptionCost =
                calculateMonthlySubscriptionCost(
                        merchantGroups
                );

        if (subscriptionCount > 0) {

            insights.add(
                    createInsight(
                            "SUBSCRIPTION",
                            "MEDIUM",
                            "Recurring subscriptions detected",
                            "The system identified "
                                    + subscriptionCount
                                    + " likely recurring subscription"
                                    + (
                                        subscriptionCount == 1
                                            ? ""
                                            : "s"
                                    )
                                    + " costing approximately "
                                    + formatCurrency(
                                            monthlySubscriptionCost
                                    )
                                    + " per month.",
                            monthlySubscriptionCost,
                            "SUBSCRIPTION",
                            null
                    )
            );

            financialRisks.add(
                    "Recurring subscriptions currently cost approximately "
                            + formatCurrency(
                                    monthlySubscriptionCost
                            )
                            + " per month."
            );

            priorityActions.add(
                    "Review recurring subscriptions and cancel "
                            + "services that are no longer needed."
            );
        }

        /*
         * --------------------------------------------------------
         * FINAL RESPONSE
         * --------------------------------------------------------
         */

        response.setInsights(
                insights
        );

        response.setPositiveBehaviours(
                removeDuplicates(
                        positiveBehaviours
                )
        );

        response.setFinancialRisks(
                removeDuplicates(
                        financialRisks
                )
        );

        response.setPriorityActions(
                removeDuplicates(
                        priorityActions
                )
        );

        return response;
    }

    /*
     * ============================================================
     * FILTER TRANSACTIONS
     * ============================================================
     */

    private List<Transaction> filterTransactions(
            List<Transaction> transactions,
            FinancialProfile financialProfile) {

        List<Transaction> result =
                new ArrayList<>();

        if (transactions == null
                || financialProfile == null
                || financialProfile.getId() == null) {

            return result;
        }

        for (Transaction transaction : transactions) {

            if (transaction == null) {
                continue;
            }

            if (transaction.getFinancialProfile() == null) {
                continue;
            }

            if (transaction.getFinancialProfile().getId() == null) {
                continue;
            }

            if (!financialProfile.getId().equals(
                    transaction
                            .getFinancialProfile()
                            .getId()
            )) {
                continue;
            }

            if (transaction.getAmount() == null) {
                continue;
            }

            result.add(transaction);
        }

        return result;
    }

    /*
     * ============================================================
     * METRICS
     * ============================================================
     */

    private FinancialMetrics calculateMetrics(
            List<Transaction> transactions) {

        FinancialMetrics metrics =
                new FinancialMetrics();

        if (transactions == null) {
            return metrics;
        }

        for (Transaction transaction : transactions) {

            if (transaction == null
                    || transaction.getAmount() == null
                    || transaction.getType() == null) {

                continue;
            }

            BigDecimal amount =
                    transaction
                            .getAmount()
                            .abs();

            if (transaction.getType()
                    == TransactionType.INCOME) {

                metrics.totalIncome =
                        metrics.totalIncome.add(
                                amount
                        );

            } else if (transaction.getType()
                    == TransactionType.EXPENSE) {

                metrics.totalExpense =
                        metrics.totalExpense.add(
                                amount
                        );
            }
        }

        metrics.totalIncome =
                scale(
                        metrics.totalIncome
                );

        metrics.totalExpense =
                scale(
                        metrics.totalExpense
                );

        metrics.totalSavings =
                scale(
                        metrics.totalIncome.subtract(
                                metrics.totalExpense
                        )
                );

        return metrics;
    }

    /*
     * ============================================================
     * SAVINGS RATE
     * ============================================================
     */

    private BigDecimal calculateSavingsRate(
            BigDecimal income,
            BigDecimal savings) {

        if (income == null
                || savings == null
                || income.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return savings
                .divide(
                        income,
                        4,
                        RoundingMode.HALF_UP
                )
                .multiply(
                        ONE_HUNDRED
                )
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                );
    }

    /*
     * ============================================================
     * HEALTH SCORE
     * ============================================================
     */

    private int calculateFinancialHealthScore(
            FinancialMetrics metrics,
            BigDecimal savingsRate) {

        int score = 50;

        if (savingsRate.compareTo(
                STRONG_SAVINGS_RATE
        ) >= 0) {

            score += 30;

        } else if (savingsRate.compareTo(
                MODERATE_SAVINGS_RATE
        ) >= 0) {

            score += 15;
        }

        if (metrics.totalIncome.compareTo(
                BigDecimal.ZERO
        ) > 0) {

            BigDecimal expenseRatio =
                    metrics.totalExpense
                            .divide(
                                    metrics.totalIncome,
                                    4,
                                    RoundingMode.HALF_UP
                            )
                            .multiply(
                                    ONE_HUNDRED
                            );

            if (expenseRatio.compareTo(
                    BigDecimal.valueOf(50)
            ) <= 0) {

                score += 15;

            } else if (expenseRatio.compareTo(
                    BigDecimal.valueOf(80)
            ) <= 0) {

                score += 5;

            } else {

                score -= 15;
            }
        }

        return Math.max(
                0,
                Math.min(
                        score,
                        100
                )
        );
    }

    /*
     * ============================================================
     * HEALTH STATUS
     * ============================================================
     */

    private String determineHealthStatus(
            int score) {

        if (score >= 80) {
            return "EXCELLENT";
        }

        if (score >= 65) {
            return "GOOD";
        }

        if (score >= 50) {
            return "FAIR";
        }

        return "NEEDS_ATTENTION";
    }

    /*
     * ============================================================
     * SUMMARY
     * ============================================================
     */

    private String buildOverallSummary(
            FinancialMetrics metrics,
            BigDecimal savingsRate,
            int healthScore) {

        return "FinSight assessed your financial health as "
                + determineHealthStatus(
                        healthScore
                )
                + " with a financial health score of "
                + healthScore
                + "/100. "
                + "Total income was "
                + formatCurrency(
                        metrics.totalIncome
                )
                + ", total expenses were "
                + formatCurrency(
                        metrics.totalExpense
                )
                + ", and total savings were "
                + formatCurrency(
                        metrics.totalSavings
                )
                + ". Your savings rate was "
                + formatAmount(
                        savingsRate
                )
                + "%.";
    }

    /*
     * ============================================================
     * CATEGORY SPENDING
     * ============================================================
     */

    private List<CategorySpending> calculateCategorySpending(
            List<Transaction> transactions,
            BigDecimal totalExpense) {

        Map<String, BigDecimal> categoryTotals =
                new LinkedHashMap<>();

        if (transactions == null) {
            return new ArrayList<>();
        }

        for (Transaction transaction : transactions) {

            if (transaction == null
                    || transaction.getType()
                    != TransactionType.EXPENSE
                    || transaction.getAmount() == null) {

                continue;
            }

            String category =
                    transaction.getCategory() == null
                            ? "UNCATEGORIZED"
                            : transaction
                                    .getCategory()
                                    .name();

            BigDecimal amount =
                    transaction
                            .getAmount()
                            .abs();

            /*
             * Explicit lambda instead of BigDecimal::add.
             *
             * This avoids the IDE null-safety warning caused by
             * the method reference and Eclipse's @NonNull analysis.
             */
            categoryTotals.merge(
                    category,
                    amount,
                    (existingValue, newValue) ->
                            existingValue.add(newValue)
            );
        }

        List<CategorySpending> result =
                new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry
                : categoryTotals.entrySet()) {

            BigDecimal percentage =
                    BigDecimal.ZERO;

            if (totalExpense != null
                    && totalExpense.compareTo(
                            BigDecimal.ZERO
                    ) > 0) {

                percentage =
                        entry.getValue()
                                .divide(
                                        totalExpense,
                                        4,
                                        RoundingMode.HALF_UP
                                )
                                .multiply(
                                        ONE_HUNDRED
                                )
                                .setScale(
                                        2,
                                        RoundingMode.HALF_UP
                                );
            }

            CategorySpending categorySpending =
                    new CategorySpending();

            categorySpending.category =
                    entry.getKey();

            categorySpending.amount =
                    scale(
                            entry.getValue()
                    );

            categorySpending.percentage =
                    percentage;

            result.add(
                    categorySpending
            );
        }

        result.sort(
                Comparator.comparing(
                        (CategorySpending value) ->
                                value.amount,
                        Comparator.reverseOrder()
                )
        );

        return result;
    }

    /*
     * ============================================================
     * LARGEST EXPENSE
     * ============================================================
     */

    private Transaction findLargestExpense(
            List<Transaction> transactions) {

        if (transactions == null) {
            return null;
        }

        return transactions.stream()
                .filter(
                        transaction ->
                                transaction != null
                                && transaction.getType()
                                    == TransactionType.EXPENSE
                                && transaction.getAmount() != null
                )
                .max(
                        Comparator.comparing(
                                transaction ->
                                        transaction
                                                .getAmount()
                                                .abs()
                        )
                )
                .orElse(null);
    }

    /*
     * ============================================================
     * MERCHANT GROUPING
     * ============================================================
     */

    private Map<String, List<Transaction>> groupByMerchant(
            List<Transaction> transactions) {

        Map<String, List<Transaction>> result =
                new LinkedHashMap<>();

        if (transactions == null) {
            return result;
        }

        for (Transaction transaction : transactions) {

            if (transaction == null
                    || transaction.getType()
                    != TransactionType.EXPENSE
                    || transaction.getDescription() == null) {

                continue;
            }

            String merchant =
                    normalizeMerchant(
                            transaction.getDescription()
                    );

            if (merchant.isBlank()) {
                continue;
            }

            result.computeIfAbsent(
                    merchant,
                    key -> new ArrayList<>()
            ).add(
                    transaction
            );
        }

        return result;
    }

    /*
     * ============================================================
     * SUBSCRIPTION COUNT
     * ============================================================
     */

    private int countLikelySubscriptions(
            Map<String, List<Transaction>> merchantGroups) {

        if (merchantGroups == null) {
            return 0;
        }

        int count = 0;

        for (Map.Entry<String, List<Transaction>> entry
                : merchantGroups.entrySet()) {

            String merchant =
                    entry.getKey();

            List<Transaction> transactions =
                    entry.getValue();

            if (merchant == null
                    || transactions == null
                    || transactions.size() < 3) {

                continue;
            }

            if (!isKnownSubscriptionMerchant(
                    merchant
            )) {

                continue;
            }

            if (!hasMonthlyPattern(
                    transactions
            )) {

                continue;
            }

            count++;
        }

        return count;
    }

    /*
     * ============================================================
     * SUBSCRIPTION COST
     * ============================================================
     */

    private BigDecimal calculateMonthlySubscriptionCost(
            Map<String, List<Transaction>> merchantGroups) {

        BigDecimal total =
                BigDecimal.ZERO;

        if (merchantGroups == null) {

            return total.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        for (Map.Entry<String, List<Transaction>> entry
                : merchantGroups.entrySet()) {

            String merchant =
                    entry.getKey();

            List<Transaction> transactions =
                    entry.getValue();

            if (merchant == null
                    || transactions == null
                    || transactions.size() < 3
                    || !isKnownSubscriptionMerchant(
                            merchant
                    )
                    || !hasMonthlyPattern(
                            transactions
                    )) {

                continue;
            }

            BigDecimal average =
                    calculateAverageAmount(
                            transactions
                    );

            total =
                    total.add(
                            average
                    );
        }

        return scale(total);
    }

    /*
     * ============================================================
     * KNOWN SUBSCRIPTIONS
     * ============================================================
     */

    private boolean isKnownSubscriptionMerchant(
            String merchant) {

        if (merchant == null
                || merchant.isBlank()) {

            return false;
        }

        String value =
                merchant.toUpperCase(
                        Locale.ROOT
                );

        return value.contains("NETFLIX")
                || value.contains("SPOTIFY")
                || value.contains("DISNEY PLUS")
                || value.contains("DISNEY+")
                || value.contains("YOUTUBE PREMIUM")
                || value.contains("APPLE MUSIC")
                || value.contains("AMAZON PRIME")
                || value.contains("ADOBE")
                || value.contains("CANVA")
                || value.contains("MICROSOFT 365")
                || value.contains("GOOGLE ONE")
                || value.contains("DROPBOX")
                || value.contains("NOTION")
                || value.contains("CHATGPT")
                || value.contains("OPENAI")
                || value.contains("CLAUDE")
                || value.contains("GITHUB");
    }

    /*
     * ============================================================
     * MONTHLY PATTERN
     * ============================================================
     */

    private boolean hasMonthlyPattern(
            List<Transaction> transactions) {

        if (transactions == null
                || transactions.size() < 3) {

            return false;
        }

        List<LocalDate> dates =
                transactions.stream()
                        .filter(
                                transaction ->
                                        transaction != null
                                        && transaction
                                            .getTransactionDate()
                                            != null
                        )
                        .map(
                                transaction ->
                                        transaction
                                            .getTransactionDate()
                                            .toLocalDate()
                        )
                        .sorted()
                        .toList();

        if (dates.size() < 3) {
            return false;
        }

        List<Long> intervals =
                new ArrayList<>();

        for (int i = 1;
                i < dates.size();
                i++) {

            long days =
                    java.time.temporal.ChronoUnit
                            .DAYS
                            .between(
                                    dates.get(i - 1),
                                    dates.get(i)
                            );

            if (days > 0) {
                intervals.add(days);
            }
        }

        if (intervals.size() < 2) {
            return false;
        }

        double average =
                intervals.stream()
                        .mapToLong(
                                value -> value.longValue()
                        )
                        .average()
                        .orElse(0.0);

        return average >= 25.0
                && average <= 35.0;
    }

    /*
     * ============================================================
     * AVERAGE AMOUNT
     * ============================================================
     */

    private BigDecimal calculateAverageAmount(
            List<Transaction> transactions) {

        if (transactions == null
                || transactions.isEmpty()) {

            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        BigDecimal total =
                BigDecimal.ZERO;

        int count = 0;

        for (Transaction transaction
                : transactions) {

            if (transaction == null
                    || transaction.getAmount() == null) {

                continue;
            }

            total =
                    total.add(
                            transaction
                                    .getAmount()
                                    .abs()
                    );

            count++;
        }

        if (count == 0) {

            return BigDecimal.ZERO.setScale(
                    2,
                    RoundingMode.HALF_UP
            );
        }

        return total.divide(
                BigDecimal.valueOf(count),
                2,
                RoundingMode.HALF_UP
        );
    }

    /*
     * ============================================================
     * INSIGHT CREATION
     * ============================================================
     */

    private FinancialInsightResponse.Insight createInsight(
            String type,
            String priority,
            String title,
            String message,
            BigDecimal relatedAmount,
            String relatedCategory,
            String relatedMerchant) {

        FinancialInsightResponse.Insight insight =
                new FinancialInsightResponse.Insight();

        insight.setType(type);
        insight.setPriority(priority);
        insight.setTitle(title);
        insight.setMessage(message);

        insight.setRelatedAmount(
                scaleNullable(
                        relatedAmount
                )
        );

        insight.setRelatedCategory(
                relatedCategory
        );

        insight.setRelatedMerchant(
                relatedMerchant
        );

        return insight;
    }

    /*
     * ============================================================
     * NORMALIZE MERCHANT
     * ============================================================
     */

    private String normalizeMerchant(
            String description) {

        if (description == null) {
            return "";
        }

        String normalized =
                description
                        .trim()
                        .toUpperCase(
                                Locale.ROOT
                        );

        normalized =
                normalized.replaceAll(
                        "\\b\\d{4,}\\b",
                        ""
                );

        normalized =
                normalized.replaceAll(
                        "[^A-Z0-9+ ]",
                        " "
                );

        normalized =
                normalized.replaceAll(
                        "\\s+",
                        " "
                )
                .trim();

        return normalized;
    }

    /*
     * ============================================================
     * DUPLICATE REMOVAL
     * ============================================================
     */

    private List<String> removeDuplicates(
            List<String> values) {

        if (values == null
                || values.isEmpty()) {

            return new ArrayList<>();
        }

        return new ArrayList<>(
                new java.util.LinkedHashSet<>(
                        values
                )
        );
    }

    /*
     * ============================================================
     * FORMATTING
     * ============================================================
     */

    private BigDecimal scale(
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

    private BigDecimal scaleNullable(
            BigDecimal value) {

        if (value == null) {
            return null;
        }

        return scale(value);
    }

    private String formatCurrency(
            BigDecimal value) {

        if (value == null) {
            return "RM0.00";
        }

        return "RM"
                + value
                    .setScale(
                            2,
                            RoundingMode.HALF_UP
                    )
                    .toPlainString();
    }

    private String formatAmount(
            BigDecimal value) {

        if (value == null) {
            return "0.00";
        }

        return value
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }

    private String safeDescription(
            Transaction transaction) {

        if (transaction == null
                || transaction.getDescription() == null
                || transaction.getDescription().isBlank()) {

            return "Unknown transaction";
        }

        return transaction
                .getDescription()
                .trim();
    }

    /*
     * ============================================================
     * AUTHENTICATED USER
     * ============================================================
     */

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

        String email =
                authentication.getName();

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found."
                        )
                );
    }

    /*
     * ============================================================
     * FINANCIAL PROFILE
     * ============================================================
     */

    private FinancialProfile getFinancialProfile(
            User user) {

        if (user == null) {

            throw new IllegalStateException(
                    "User must not be null."
            );
        }

        return financialProfileRepository
                .findByUser(user)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Financial profile not found."
                        )
                );
    }

    /*
     * ============================================================
     * INTERNAL METRICS
     * ============================================================
     */

    private static class FinancialMetrics {

        private BigDecimal totalIncome =
                BigDecimal.ZERO;

        private BigDecimal totalExpense =
                BigDecimal.ZERO;

        private BigDecimal totalSavings =
                BigDecimal.ZERO;
    }

    private static class CategorySpending {

        private String category;

        private BigDecimal amount;

        private BigDecimal percentage;
    }
}

