package com.finsight.backend.enums;

/**
 * Spending/income categories used across the app. The AI classifier's output strings
 * (see transaction_classifier.py) must match these names exactly.
 */
public enum TransactionCategory {

    FOOD,
    GROCERIES,
    TRANSPORTATION,
    HOUSING,
    UTILITIES,
    HEALTHCARE,
    ENTERTAINMENT,
    SHOPPING,
    EDUCATION,
    SUBSCRIPTION,
    TRAVEL,
    PERSONAL_CARE,
    FINANCIAL,
    INVESTMENT,
    SALARY,
    OTHER

}