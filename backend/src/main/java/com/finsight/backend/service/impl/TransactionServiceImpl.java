/**
 * FinSight File Notes: Defines a business-service operation used by the backend to keep application logic separate from controllers.
 */
package com.finsight.backend.service.impl;

import com.finsight.backend.dto.transaction.TransactionCreateRequest;
import com.finsight.backend.dto.transaction.TransactionResponse;
import com.finsight.backend.dto.transaction.TransactionUpdateRequest;
import com.finsight.backend.entity.FinancialProfile;
import com.finsight.backend.entity.Transaction;
import com.finsight.backend.entity.UploadHistory;
import com.finsight.backend.entity.User;
import com.finsight.backend.repository.FinancialProfileRepository;
import com.finsight.backend.repository.TransactionRepository;
import com.finsight.backend.repository.UploadHistoryRepository;
import com.finsight.backend.repository.UserRepository;
import com.finsight.backend.service.TransactionService;

import lombok.RequiredArgsConstructor;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    private final UserRepository userRepository;

    private final FinancialProfileRepository financialProfileRepository;

    private final UploadHistoryRepository uploadHistoryRepository;


    /*
     * ============================================================
     * CREATE TRANSACTION
     * ============================================================
     */
    @Override
    public TransactionResponse createTransaction(
            TransactionCreateRequest request) {

        Objects.requireNonNull(
                request,
                "Transaction request must not be null"
        );

        User user = getAuthenticatedUser();

        FinancialProfile financialProfile =
                getFinancialProfile(user);


        Transaction transaction = new Transaction();


        /*
         * USER
         */
        transaction.setUser(user);


        /*
         * FINANCIAL PROFILE
         */
        transaction.setFinancialProfile(
                financialProfile
        );


        /*
         * TRANSACTION DATE
         *
         * Request uses LocalDate.
         * Entity uses LocalDateTime.
         */
        if (request.getTransactionDate() != null) {

            LocalDate date =
                    request.getTransactionDate();

            transaction.setTransactionDate(
                    date.atStartOfDay()
            );
        }


        /*
         * DESCRIPTION
         */
        transaction.setDescription(
                request.getDescription()
        );


        /*
         * AMOUNT
         */
        transaction.setAmount(
                request.getAmount()
        );


        /*
         * TYPE
         */
        transaction.setType(
                request.getType()
        );


        /*
         * CATEGORY
         */
        transaction.setCategory(
                request.getCategory()
        );


        /*
         * PAYMENT METHOD
         *
         * PaymentMethod is already an enum,
         * so NO String conversion is required.
         */
        transaction.setPaymentMethod(
                request.getPaymentMethod()
        );


        /*
         * UPLOAD HISTORY
         *
         * Optional.
         *
         * If uploadHistoryId is provided,
         * make sure that the upload belongs to
         * the current user's financial profile.
         */
        if (request.getUploadHistoryId() != null) {

            UploadHistory uploadHistory =
                    uploadHistoryRepository
                            .findByIdAndFinancialProfile(
                                    request.getUploadHistoryId(),
                                    financialProfile
                            )
                            .orElseThrow(() ->
                                    new RuntimeException(
                                            "Upload history not found for the current financial profile."
                                    )
                            );

            transaction.setUploadHistory(
                    uploadHistory
            );
        }


        /*
         * SAVE
         */
        Transaction savedTransaction =
                transactionRepository.save(
                        Objects.requireNonNull(
                                transaction
                        )
                );


        return mapToResponse(
                Objects.requireNonNull(
                        savedTransaction
                )
        );
    }


    /*
     * ============================================================
     * GET ALL TRANSACTIONS
     * ============================================================
     */
    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getMyTransactions() {

        User user =
                getAuthenticatedUser();

        return transactionRepository
                .findByUserOrderByTransactionDateDesc(
                        user
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }


    /*
     * ============================================================
     * GET ONE TRANSACTION
     * ============================================================
     */
    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(
            Long transactionId) {

        User user =
                getAuthenticatedUser();


        Transaction transaction =
                transactionRepository
                        .findByIdAndUser(
                                transactionId,
                                user
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Transaction not found."
                                )
                        );


        return mapToResponse(
                transaction
        );
    }


    /*
     * ============================================================
     * UPDATE TRANSACTION
     * ============================================================
     */
    @Override
    public TransactionResponse updateTransaction(
            Long transactionId,
            TransactionUpdateRequest request) {

        Objects.requireNonNull(
                request,
                "Transaction update request must not be null"
        );


        User user =
                getAuthenticatedUser();


        Transaction transaction =
                transactionRepository
                        .findByIdAndUser(
                                transactionId,
                                user
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Transaction not found."
                                )
                        );


        /*
         * DATE
         */
        if (request.getTransactionDate() != null) {

            transaction.setTransactionDate(
                    request
                            .getTransactionDate()
                            .atStartOfDay()
            );
        }


        /*
         * DESCRIPTION
         */
        if (request.getDescription() != null
                && !request
                        .getDescription()
                        .isBlank()) {

            transaction.setDescription(
                    request
                            .getDescription()
                            .trim()
            );
        }


        /*
         * AMOUNT
         */
        if (request.getAmount() != null) {

            transaction.setAmount(
                    request.getAmount()
            );
        }


        /*
         * TYPE
         */
        if (request.getType() != null) {

            transaction.setType(
                    request.getType()
            );
        }


        /*
         * CATEGORY
         */
        if (request.getCategory() != null) {

            transaction.setCategory(
                    request.getCategory()
            );
        }


        /*
         * PAYMENT METHOD
         */
        if (request.getPaymentMethod() != null) {

            transaction.setPaymentMethod(
                    request.getPaymentMethod()
            );
        }


        /*
         * SAVE
         */
        Transaction updatedTransaction =
                transactionRepository.save(
                        Objects.requireNonNull(
                                transaction
                        )
                );


        return mapToResponse(
                Objects.requireNonNull(
                        updatedTransaction
                )
        );
    }


    /*
     * ============================================================
     * DELETE TRANSACTION
     * ============================================================
     */
    @Override
    public void deleteTransaction(
            Long transactionId) {

        User user =
                getAuthenticatedUser();


        Transaction transaction =
                transactionRepository
                        .findByIdAndUser(
                                transactionId,
                                user
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Transaction not found."
                                )
                        );


        transactionRepository.delete(
                Objects.requireNonNull(
                        transaction
                )
        );
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
                                "Financial profile not found. "
                                + "Please create your financial profile "
                                + "before adding transactions."
                        )
                );
    }


    /*
     * ============================================================
     * ENTITY -> RESPONSE DTO
     * ============================================================
     */
    private TransactionResponse mapToResponse(
            Transaction transaction) {

        Objects.requireNonNull(
                transaction,
                "Transaction must not be null"
        );


        return new TransactionResponse(

                transaction.getId(),

                transaction.getTransactionDate(),

                transaction.getDescription(),

                transaction.getAmount(),

                transaction.getType(),

                transaction.getCategory(),

                transaction.getPaymentMethod(),

                transaction.getCreatedAt(),

                transaction.getUpdatedAt()
        );
    }
}