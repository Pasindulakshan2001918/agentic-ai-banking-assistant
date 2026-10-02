package com.lunorlabs.bank.domain;

import java.math.BigDecimal;

/** One row of the product matrix. Nullable limits mean "no limit of that kind". */
public record ProductRule(String actionType, String sourceProduct, String rail, boolean enabled,
                          BigDecimal minAmount, BigDecimal maxPerTxn, BigDecimal maxDaily,
                          BigDecimal otpThresholdPerTxn, BigDecimal otpThresholdDaily,
                          boolean alwaysOtp, int newPayeeCoolingHrs,
                          BigDecimal feeFlat, BigDecimal feePct) {}
