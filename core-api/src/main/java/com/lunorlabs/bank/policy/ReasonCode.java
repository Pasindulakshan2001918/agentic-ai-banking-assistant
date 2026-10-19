package com.lunorlabs.bank.policy;

/** Every code has an i18n_messages row (EN at minimum). OK has no message. */
public enum ReasonCode {
    OK,
    ACCOUNT_INACTIVE,
    PRODUCT_NOT_ALLOWED,
    BELOW_MIN_AMOUNT,
    ABOVE_TXN_LIMIT,
    ABOVE_DAILY_LIMIT,
    INSUFFICIENT_FUNDS,
    SAME_ACCOUNT_TRANSFER,
    OTP_HIGH_VALUE,
    OTP_DAILY_CUMULATIVE,
    OTP_NEW_PAYEE,
    OTP_MANDATORY_ACTION
}
