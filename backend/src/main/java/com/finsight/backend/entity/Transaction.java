/**
 * FinSight File Notes: Represents one financial transaction stored in the database.
 */
package com.finsight.backend.entity;

import com.finsight.backend.enums.PaymentMethod;
import com.finsight.backend.enums.TransactionCategory;
import com.finsight.backend.enums.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "transactions")
public class Transaction extends BaseEntity {

    /*
     * USER
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false
    )
    private User user;


    /*
     * FINANCIAL PROFILE
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "financial_profile_id",
            nullable = false
    )
    private FinancialProfile financialProfile;


    /*
     * UPLOAD HISTORY
     *
     * A transaction can either:
     *
     * 1. Be created manually
     * 2. Come from an uploaded file
     *
     * Therefore this relationship MUST be optional.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "upload_history_id",
            nullable = true
    )
    private UploadHistory uploadHistory;


    /*
     * TRANSACTION DATE
     */
    @Column(
            name = "transaction_date",
            nullable = false
    )
    private LocalDateTime transactionDate;


    /*
     * DESCRIPTION
     */
    @Column(
            nullable = false,
            length = 255
    )
    private String description;


    /*
     * AMOUNT
     */
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;


    /*
     * TRANSACTION TYPE
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private TransactionType type;


    /*
     * TRANSACTION CATEGORY
     */
    @Enumerated(EnumType.STRING)
    @Column(
            length = 50
    )
    private TransactionCategory category;


    /*
     * PAYMENT METHOD
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            length = 50
    )
    private PaymentMethod paymentMethod;
}