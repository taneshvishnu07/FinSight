package com.finsight.backend.enums;

/**
 * Kind of alert. Values stored in the database as text; keep in sync with the type
 * strings produced by the Python alert agent.
 */
public enum AlertType {

    BUDGET,

    SUBSCRIPTION,

    UNUSUAL_SPENDING,

    LARGE_TRANSACTION,

    LOW_BALANCE,

    REMINDER

}