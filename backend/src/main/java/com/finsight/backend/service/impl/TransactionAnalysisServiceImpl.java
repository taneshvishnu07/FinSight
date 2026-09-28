/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.finsight.backend.dto.analysis.TransactionAnalysisResponse;
import com.finsight.backend.entity.AnalysisJob;
import com.finsight.backend.entity.FinancialAnalysis;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.enums.AnalysisStatus;
import com.finsight.backend.enums.TransactionType;
import com.finsight.backend.repository.AnalysisJobRepository;
import com.finsight.backend.repository.FinancialAnalysisRepository;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.TransactionAnalysisService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionAnalysisServiceImpl
        implements TransactionAnalysisService {

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final FinancialProfileRepository financialProfileRepository;

    private final UploadHistoryRepository uploadHistoryRepository;

    private final AnalysisJobRepository analysisJobRepository;

    private final FinancialAnalysisRepository financialAnalysisRepository;


    /*
     * ============================================================
     * RUN TRANSACTION ANALYSIS
     * ============================================================
     */
    @Override
    public TransactionAnalysisResponse analyseUpload(
            Long uploadHistoryId) {

        Objects.requireNonNull(
                uploadHistoryId,
                "Upload history ID must not be null."
        );


        /*
         * --------------------------------------------------------
         * Get authenticated user
         * --------------------------------------------------------
         */

        User user =
                getAuthenticatedUser();


        /*
         * --------------------------------------------------------
         * Get user's financial profile
         * --------------------------------------------------------
         */

        FinancialProfile financialProfile =
                getFinancialProfile(user);


        /*
         * --------------------------------------------------------
         * Find upload
         *
         * Security:
         * The upload must belong to the current user's
         * financial profile.
         * --------------------------------------------------------
         */

        UploadHistory uploadHistory =
                uploadHistoryRepository
                        .findByIdAndFinancialProfile(
                                uploadHistoryId,
                                financialProfile
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Upload history not found."
                                )
                        );


        /*
         * --------------------------------------------------------
         * Get transactions belonging to this upload
         * --------------------------------------------------------
         */

        List<Transaction> transactions =
                transactionRepository
                        .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                                uploadHistory,
                                user
                        );


        if (transactions.isEmpty()) {

            throw new RuntimeException(
                    "No transactions were found for this upload."
            );
        }


        /*
         * --------------------------------------------------------
         * Create analysis job
         * --------------------------------------------------------
         */

        AnalysisJob analysisJob =
                new AnalysisJob();

        analysisJob.setUploadHistory(
                uploadHistory
        );

        analysisJob.setStatus(
                AnalysisStatus.PROCESSING
        );

        analysisJob.setStartedAt(
                LocalDateTime.now()
        );


        AnalysisJob savedJob =
                analysisJobRepository.save(
                        analysisJob
                );


        /*
         * --------------------------------------------------------
         * Create FinancialAnalysis
         * --------------------------------------------------------
         */

        FinancialAnalysis analysis =
                new FinancialAnalysis();

        analysis.setFinancialProfile(
                financialProfile
        );

        analysis.setAnalysisJob(
                savedJob
        );

        // Keep both sides of the bidirectional relationship in sync
        // so downstream services can immediately reuse this record.
        savedJob.setFinancialAnalysis(
                analysis
        );

        analysis.setStatus(
                AnalysisStatus.PROCESSING
        );

        analysis.setAnalysedAt(
                LocalDateTime.now()
        );

        /*
         * Persist the processing record BEFORE any downstream
         * analysis layer runs. Recommendation/alert services look
         * up the FinancialAnalysis through the AnalysisJob. If the
         * record is not persisted here, those services may create a
         * second FinancialAnalysis for the same profile/job.
         */
        FinancialAnalysis persistedAnalysis =
                financialAnalysisRepository.saveAndFlush(
                        analysis
                );


        /*
         * --------------------------------------------------------
         * Calculate totals
         * --------------------------------------------------------
         */

        BigDecimal totalIncome =
                BigDecimal.ZERO;

        BigDecimal totalExpense =
                BigDecimal.ZERO;


        int incomeCount = 0;

        int expenseCount = 0;


        /*
         * --------------------------------------------------------
         * Category spending
         * --------------------------------------------------------
         */

        Map<String, BigDecimal> spendingByCategory =
                new LinkedHashMap<>();


        /*
         * --------------------------------------------------------
         * Largest expense
         * --------------------------------------------------------
         */

        BigDecimal largestExpense =
                BigDecimal.ZERO;

        String largestExpenseDescription =
                null;


        /*
         * --------------------------------------------------------
         * Date range
         * --------------------------------------------------------
         */

        LocalDate analysedFrom =
                null;

        LocalDate analysedTo =
                null;


        /*
         * --------------------------------------------------------
         * Process transactions
         * --------------------------------------------------------
         */

        for (Transaction transaction : transactions) {

            if (transaction == null) {
                continue;
            }


            BigDecimal amount =
                    transaction.getAmount();


            if (amount == null) {
                continue;
            }


            amount =
                    amount.abs();


            /*
             * ----------------------------------------------------
             * Date
             * ----------------------------------------------------
             */

            if (transaction.getTransactionDate() != null) {

                LocalDate transactionDate =
                        transaction
                                .getTransactionDate()
                                .toLocalDate();


                if (analysedFrom == null
                        || transactionDate.isBefore(
                                analysedFrom)) {

                    analysedFrom =
                            transactionDate;
                }


                if (analysedTo == null
                        || transactionDate.isAfter(
                                analysedTo)) {

                    analysedTo =
                            transactionDate;
                }
            }


            /*
             * ----------------------------------------------------
             * Income
             * ----------------------------------------------------
             */

            if (transaction.getType()
                    == TransactionType.INCOME) {

                totalIncome =
                        totalIncome.add(
                                amount
                        );

                incomeCount++;

                continue;
            }


            /*
             * ----------------------------------------------------
             * Expense
             * ----------------------------------------------------
             */

            if (transaction.getType()
                    == TransactionType.EXPENSE) {

                totalExpense =
                        totalExpense.add(
                                amount
                        );

                expenseCount++;


                /*
                 * -----------------------------------------------
                 * Largest expense
                 * -----------------------------------------------
                 */

                if (!isEssentialExpense(transaction)
                        && amount.compareTo(largestExpense) > 0) {

                    largestExpense =
                            amount;

                    largestExpenseDescription =
                            transaction.getDescription();
                }


                /*
                 * -----------------------------------------------
                 * Spending by category
                 *
                 * Explicit lambda is used instead of
                 * BigDecimal::add to avoid Eclipse null-safety
                 * warnings.
                 * -----------------------------------------------
                 */

                String category =
                        transaction.getCategory() != null
                                ? transaction
                                        .getCategory()
                                        .name()
                                : "OTHER";


                spendingByCategory.merge(
                        category,
                        amount,
                        (existingAmount, newAmount) ->
                                existingAmount.add(newAmount)
                );
            }
        }


        /*
         * --------------------------------------------------------
         * Savings
         *
         * Income - Expenses
         * --------------------------------------------------------
         */

        BigDecimal totalSavings =
                totalIncome.subtract(
                        totalExpense
                );


        /*
         * --------------------------------------------------------
         * Average expense
         * --------------------------------------------------------
         */

        BigDecimal averageExpense =
                BigDecimal.ZERO;


        if (expenseCount > 0) {

            averageExpense =
                    totalExpense.divide(
                            BigDecimal.valueOf(
                                    expenseCount
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }


        /*
         * --------------------------------------------------------
         * Basic recurring subscription count
         *
         * This is intentionally conservative.
         *
         * The dedicated Subscription Agent will later perform
         * proper recurring-payment detection.
         * --------------------------------------------------------
         */

        int recurringSubscriptions =
                countSubscriptionTransactions(
                        transactions
                );


        /*
         * --------------------------------------------------------
         * Basic unusual transaction count
         *
         * The dedicated Unusual Spending Agent will later
         * perform more advanced anomaly detection.
         *
         * For now, this counts expenses that are significantly
         * larger than the average expense.
         * --------------------------------------------------------
         */

        int unusualTransactions =
                countUnusualTransactions(
                        transactions,
                        averageExpense
                );


        /*
         * --------------------------------------------------------
         * Recommendation count
         *
         * Recommendations are generated by the recommendation
         * layer later.
         *
         * Therefore it starts at zero.
         * --------------------------------------------------------
         */

        int recommendationCount = 0;


        /*
         * --------------------------------------------------------
         * Generate summary
         * --------------------------------------------------------
         */

        String summary =
                generateSummary(
                        totalIncome,
                        totalExpense,
                        totalSavings,
                        expenseCount,
                        averageExpense,
                        largestExpense,
                        largestExpenseDescription,
                        financialProfile,
                        analysedFrom,
                        analysedTo
                );


        /*
         * --------------------------------------------------------
         * Store FinancialAnalysis
         * --------------------------------------------------------
         */

        analysis.setStatus(
                AnalysisStatus.COMPLETED
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

        analysis.setRecurringSubscriptions(
                recurringSubscriptions
        );

        analysis.setUnusualTransactions(
                unusualTransactions
        );

        analysis.setRecommendationCount(
                recommendationCount
        );

        analysis.setSummary(
                summary
        );


        analysis = persistedAnalysis;

        FinancialAnalysis savedAnalysis =
                financialAnalysisRepository.save(
                        analysis
                );


        /*
         * --------------------------------------------------------
         * Complete analysis job
         * --------------------------------------------------------
         */

        savedJob.setStatus(
                AnalysisStatus.COMPLETED
        );

        savedJob.setCompletedAt(
                LocalDateTime.now()
        );

        savedJob.setErrorMessage(
                null
        );


        analysisJobRepository.save(
                savedJob
        );


        /*
         * --------------------------------------------------------
         * Return response
         * --------------------------------------------------------
         */

        return buildResponse(
                savedAnalysis,
                savedJob,
                uploadHistory,
                transactions,
                analysedFrom,
                analysedTo,
                totalIncome,
                totalExpense,
                totalSavings,
                incomeCount,
                expenseCount,
                averageExpense,
                largestExpense,
                largestExpenseDescription,
                recurringSubscriptions,
                unusualTransactions,
                recommendationCount,
                spendingByCategory,
                summary
        );
    }


    /*
     * ============================================================
     * GET ANALYSIS BY ID
     * ============================================================
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionAnalysisResponse getAnalysisById(
            Long analysisId) {

        Objects.requireNonNull(
                analysisId,
                "Analysis ID must not be null."
        );


        User user =
                getAuthenticatedUser();


        FinancialProfile financialProfile =
                getFinancialProfile(user);


        FinancialAnalysis analysis =
                financialAnalysisRepository
                        .findById(
                                analysisId
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Financial analysis not found."
                                )
                        );


        /*
         * Security check.
         */

        if (analysis.getFinancialProfile() == null
                || analysis
                        .getFinancialProfile()
                        .getId() == null
                || !analysis
                        .getFinancialProfile()
                        .getId()
                        .equals(
                                financialProfile.getId()
                        )) {

            throw new RuntimeException(
                    "Financial analysis does not belong to the current user."
            );
        }


        if (analysis.getStatus() != AnalysisStatus.COMPLETED) {
            throw new RuntimeException(
                    "Analysis results are available only after the analysis is completed."
            );
        }


        AnalysisJob analysisJob =
                analysis.getAnalysisJob();


        if (analysisJob == null) {

            throw new RuntimeException(
                    "Analysis job was not found."
            );
        }


        UploadHistory uploadHistory =
                analysisJob.getUploadHistory();


        if (uploadHistory == null) {

            throw new RuntimeException(
                    "Upload history was not found."
            );
        }


        List<Transaction> transactions =
                transactionRepository
                        .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                                uploadHistory,
                                user
                        );


        return rebuildResponse(
                analysis,
                analysisJob,
                uploadHistory,
                transactions
        );
    }


    /*
     * ============================================================
     * GET LATEST ANALYSIS
     * ============================================================
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionAnalysisResponse getLatestAnalysis() {

        User user =
                getAuthenticatedUser();


        FinancialProfile financialProfile =
                getFinancialProfile(user);


        FinancialAnalysis analysis =
                financialAnalysisRepository
                        .findFirstByFinancialProfileAndStatusOrderByAnalysedAtDesc(
                                financialProfile,
                                AnalysisStatus.COMPLETED
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "No financial analysis has been generated yet."
                                )
                        );


        AnalysisJob analysisJob =
                analysis.getAnalysisJob();


        if (analysisJob == null) {

            throw new RuntimeException(
                    "Analysis job was not found."
            );
        }


        UploadHistory uploadHistory =
                analysisJob.getUploadHistory();


        if (uploadHistory == null) {

            throw new RuntimeException(
                    "Upload history was not found."
            );
        }


        List<Transaction> transactions =
                transactionRepository
                        .findByUploadHistoryAndUserOrderByTransactionDateDesc(
                                uploadHistory,
                                user
                        );


        return rebuildResponse(
                analysis,
                analysisJob,
                uploadHistory,
                transactions
        );
    }


    /*
     * ============================================================
     * COUNT SUBSCRIPTION TRANSACTIONS
     * ============================================================
     */
    private int countSubscriptionTransactions(
            List<Transaction> transactions) {

        int count = 0;


        for (Transaction transaction : transactions) {

            if (transaction == null) {
                continue;
            }


            if (transaction.getType()
                    != TransactionType.EXPENSE) {

                continue;
            }


            if (transaction.getCategory() != null
                    && transaction
                            .getCategory()
                            .name()
                            .equals("SUBSCRIPTION")) {

                count++;
            }
        }


        return count;
    }


    /*
     * ============================================================
     * COUNT BASIC UNUSUAL TRANSACTIONS
     * ============================================================
     */
    private int countUnusualTransactions(
            List<Transaction> transactions,
            BigDecimal averageExpense) {

        if (averageExpense == null
                || averageExpense.compareTo(
                        BigDecimal.ZERO
                ) <= 0) {

            return 0;
        }


        /*
         * For the initial deterministic layer,
         * an expense more than 2.5 times the average
         * expense is considered unusually large.
         *
         * The dedicated anomaly agent will later replace
         * this with Isolation Forest.
         */

        BigDecimal threshold =
                averageExpense.multiply(
                        BigDecimal.valueOf(2.5)
                );


        int count = 0;


        for (Transaction transaction : transactions) {

            if (transaction == null) {
                continue;
            }


            if (transaction.getType()
                    != TransactionType.EXPENSE) {

                continue;
            }


            BigDecimal amount =
                    transaction.getAmount();


            if (amount == null) {
                continue;
            }


            if (isEssentialExpense(transaction)) {
                continue;
            }

            if (amount.abs().compareTo(
                    threshold
            ) > 0) {

                count++;
            }
        }


        return count;
    }


    private boolean isEssentialExpense(Transaction transaction) {
        if (transaction == null || transaction.getType() != TransactionType.EXPENSE) {
            return false;
        }
        if (transaction.getCategory() == null) {
            return false;
        }
        return switch (transaction.getCategory()) {
            case HOUSING, UTILITIES, GROCERIES, TRANSPORTATION, HEALTHCARE, EDUCATION, FINANCIAL -> true;
            default -> false;
        };
    }

    /*
     * ============================================================
     * GENERATE SUMMARY
     * ============================================================
     */
    private String generateSummary(
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalSavings,
            int expenseCount,
            BigDecimal averageExpense,
            BigDecimal largestExpense,
            String largestExpenseDescription,
            FinancialProfile financialProfile,
            LocalDate analysedFrom,
            LocalDate analysedTo) {


        StringBuilder summary =
                new StringBuilder();


        summary.append(
                "The analysed transactions contain total income of "
        );

        summary.append(
                formatAmount(totalIncome)
        );

        summary.append(
                " and total expenses of "
        );

        summary.append(
                formatAmount(totalExpense)
        );

        summary.append(
                ". "
        );


        summary.append(
                "The resulting savings are "
        );

        summary.append(
                formatAmount(totalSavings)
        );

        summary.append(
                ". "
        );


        if (expenseCount > 0) {

            summary.append(
                    "The average expense is "
            );

            summary.append(
                    formatAmount(averageExpense)
            );

            summary.append(
                    ". "
            );
        }


        if (financialProfile != null) {
            java.util.Map<String, BigDecimal> configuredBudgets = new java.util.HashMap<>();
            if (financialProfile.getMonthlyBudgets() != null) {
                financialProfile.getMonthlyBudgets().forEach(budget -> {
                    if (budget != null && budget.getBudgetMonth() != null && budget.getAmount() != null) {
                        configuredBudgets.put(budget.getBudgetMonth(), budget.getAmount());
                    }
                });
            }

            if (!configuredBudgets.isEmpty() && analysedFrom != null && analysedTo != null) {
                BigDecimal configuredTotal = BigDecimal.ZERO;
                long monthCount = 0;
                java.time.YearMonth cursor = java.time.YearMonth.from(analysedFrom);
                java.time.YearMonth endMonth = java.time.YearMonth.from(analysedTo);
                while (!cursor.isAfter(endMonth)) {
                    BigDecimal monthBudget = configuredBudgets.get(cursor.toString());
                    if (monthBudget != null && monthBudget.compareTo(BigDecimal.ZERO) > 0) {
                        configuredTotal = configuredTotal.add(monthBudget);
                        monthCount++;
                    }
                    cursor = cursor.plusMonths(1);
                }

                if (monthCount > 0) {
                    summary.append("Configured budgets were recorded for ")
                            .append(monthCount)
                            .append(" analysed month(s), with a combined budget of ")
                            .append(formatAmount(configuredTotal))
                            .append(". ");
                }
            }

            if (financialProfile.getFinancialGoalMonths() != null
                    && financialProfile.getFinancialGoalMonths() > 0
                    && analysedFrom != null
                    && analysedTo != null) {
                long months = java.time.temporal.ChronoUnit.MONTHS.between(
                        java.time.YearMonth.from(analysedFrom),
                        java.time.YearMonth.from(analysedTo)
                ) + 1;
                months = Math.max(months, 1);
                BigDecimal goalProjection = totalSavings
                        .divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(financialProfile.getFinancialGoalMonths()));
                summary.append("At the current savings pace, approximately ")
                        .append(formatAmount(goalProjection))
                        .append(" could be accumulated over the ")
                        .append(financialProfile.getFinancialGoalMonths())
                        .append("-month financial goal period. ");
            }
        }

        if (largestExpense != null
                && largestExpense.compareTo(
                        BigDecimal.ZERO
                ) > 0) {

            summary.append(
                    "The largest expense is "
            );

            summary.append(
                    formatAmount(largestExpense)
            );


            if (largestExpenseDescription != null
                    && !largestExpenseDescription.isBlank()) {

                summary.append(
                        " for "
                );

                summary.append(
                        largestExpenseDescription
                );
            }


            summary.append(
                    "."
            );
        }


        return summary.toString();
    }


    /*
     * ============================================================
     * FORMAT AMOUNT
     * ============================================================
     */
    private String formatAmount(
            BigDecimal amount) {

        if (amount == null) {
            return "0.00";
        }


        return amount
                .setScale(
                        2,
                        RoundingMode.HALF_UP
                )
                .toPlainString();
    }


    /*
     * ============================================================
     * REBUILD RESPONSE
     * ============================================================
     */
    private TransactionAnalysisResponse rebuildResponse(
            FinancialAnalysis analysis,
            AnalysisJob analysisJob,
            UploadHistory uploadHistory,
            List<Transaction> transactions) {


        BigDecimal totalIncome =
                analysis.getTotalIncome() != null
                        ? analysis.getTotalIncome()
                        : BigDecimal.ZERO;


        BigDecimal totalExpense =
                analysis.getTotalExpense() != null
                        ? analysis.getTotalExpense()
                        : BigDecimal.ZERO;


        BigDecimal totalSavings =
                analysis.getTotalSavings() != null
                        ? analysis.getTotalSavings()
                        : BigDecimal.ZERO;


        int incomeCount = 0;

        int expenseCount = 0;


        BigDecimal largestExpense =
                BigDecimal.ZERO;


        String largestExpenseDescription =
                null;


        LocalDate analysedFrom =
                null;

        LocalDate analysedTo =
                null;


        Map<String, BigDecimal> spendingByCategory =
                new LinkedHashMap<>();


        for (Transaction transaction : transactions) {

            if (transaction == null) {
                continue;
            }


            if (transaction.getTransactionDate() != null) {

                LocalDate date =
                        transaction
                                .getTransactionDate()
                                .toLocalDate();


                if (analysedFrom == null
                        || date.isBefore(analysedFrom)) {

                    analysedFrom = date;
                }


                if (analysedTo == null
                        || date.isAfter(analysedTo)) {

                    analysedTo = date;
                }
            }


            if (transaction.getType()
                    == TransactionType.INCOME) {

                incomeCount++;

                continue;
            }


            if (transaction.getType()
                    == TransactionType.EXPENSE) {

                expenseCount++;


                BigDecimal amount =
                        transaction.getAmount() != null
                                ? transaction
                                        .getAmount()
                                        .abs()
                                : BigDecimal.ZERO;


                if (!isEssentialExpense(transaction)
                        && amount.compareTo(largestExpense) > 0) {

                    largestExpense =
                            amount;

                    largestExpenseDescription =
                            transaction.getDescription();
                }


                String category =
                        transaction.getCategory() != null
                                ? transaction
                                        .getCategory()
                                        .name()
                                : "OTHER";


                /*
                 * Explicit lambda instead of BigDecimal::add
                 * to prevent Eclipse null-safety warning.
                 */

                spendingByCategory.merge(
                        category,
                        amount,
                        (existingAmount, newAmount) ->
                                existingAmount.add(newAmount)
                );
            }
        }


        BigDecimal averageExpense =
                BigDecimal.ZERO;


        if (expenseCount > 0) {

            averageExpense =
                    totalExpense.divide(
                            BigDecimal.valueOf(
                                    expenseCount
                            ),
                            2,
                            RoundingMode.HALF_UP
                    );
        }


        return buildResponse(
                analysis,
                analysisJob,
                uploadHistory,
                transactions,
                analysedFrom,
                analysedTo,
                totalIncome,
                totalExpense,
                totalSavings,
                incomeCount,
                expenseCount,
                averageExpense,
                largestExpense,
                largestExpenseDescription,
                analysis.getRecurringSubscriptions(),
                analysis.getUnusualTransactions(),
                analysis.getRecommendationCount(),
                spendingByCategory,
                analysis.getSummary()
        );
    }


    /*
     * ============================================================
     * BUILD RESPONSE
     * ============================================================
     */
    private TransactionAnalysisResponse buildResponse(
            FinancialAnalysis analysis,
            AnalysisJob analysisJob,
            UploadHistory uploadHistory,
            List<Transaction> transactions,
            LocalDate analysedFrom,
            LocalDate analysedTo,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal totalSavings,
            int incomeCount,
            int expenseCount,
            BigDecimal averageExpense,
            BigDecimal largestExpense,
            String largestExpenseDescription,
            int recurringSubscriptions,
            int unusualTransactions,
            int recommendationCount,
            Map<String, BigDecimal> spendingByCategory,
            String summary) {


        TransactionAnalysisResponse response =
                new TransactionAnalysisResponse();


        response.setAnalysisId(
                analysis.getId()
        );


        response.setAnalysisJobId(
                analysisJob.getId()
        );


        response.setUploadHistoryId(
                uploadHistory.getId()
        );


        response.setFileName(
                uploadHistory.getOriginalFileName()
        );


        response.setStatus(
                analysis.getStatus()
        );


        response.setAnalysedFrom(
                analysedFrom
        );


        response.setAnalysedTo(
                analysedTo
        );


        response.setTotalTransactions(
                transactions.size()
        );


        response.setIncomeTransactions(
                incomeCount
        );


        response.setExpenseTransactions(
                expenseCount
        );


        response.setTotalIncome(
                totalIncome
        );


        response.setTotalExpense(
                totalExpense
        );


        response.setTotalSavings(
                totalSavings
        );


        response.setAverageExpense(
                averageExpense
        );


        response.setLargestExpense(
                largestExpense
        );


        response.setLargestExpenseDescription(
                largestExpenseDescription
        );


        response.setRecurringSubscriptions(
                recurringSubscriptions
        );


        response.setUnusualTransactions(
                unusualTransactions
        );


        response.setRecommendationCount(
                recommendationCount
        );


        response.setSpendingByCategory(
                spendingByCategory
        );


        response.setSummary(
                summary
        );


        return response;
    }


    /*
     * ============================================================
     * GET AUTHENTICATED USER
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

            throw new RuntimeException(
                    "User is not authenticated."
            );
        }


        String email =
                authentication.getName();


        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Authenticated user not found."
                        )
                );
    }


    /*
     * ============================================================
     * GET FINANCIAL PROFILE
     * ============================================================
     */
    private FinancialProfile getFinancialProfile(
            User user) {

        return financialProfileRepository
                .findByUser(
                        Objects.requireNonNull(user)
                )
                .orElseThrow(() ->
                        new RuntimeException(
                                "Financial profile not found."
                        )
                );
    }
}

