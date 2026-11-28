package com.finsight.backend.enums;

/**
 * How a transaction was paid. Optional on a transaction; accepted values for CSV
 * uploads are listed on the upload instructions page.
 */
public enum PaymentMethod {

    CASH,

    DEBIT_CARD,

    CREDIT_CARD,

    BANK_TRANSFER,

    DUITNOW,

    E_WALLET,

    CHEQUE,

    OTHER

}