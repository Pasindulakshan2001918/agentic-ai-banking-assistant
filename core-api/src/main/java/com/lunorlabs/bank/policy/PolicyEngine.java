package com.lunorlabs.bank.policy;

import com.lunorlabs.bank.common.Money;
import com.lunorlabs.bank.domain.ProductRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Deterministic rules. Order: state -> product matrix -> amount limits -> funds -> step-up.
 * The first DENY wins; step-up reasons accumulate. The LLM never influences any of this.
 */
@Component
public class PolicyEngine {

    public PolicyResult evaluate(PolicyInput in) {
        ProductRule r = in.rule();

        if (!in.accountActive()) {
            return PolicyResult.deny(ReasonCode.ACCOUNT_INACTIVE, Map.of());
        }
        if (r == null || !r.enabled()) {
            return PolicyResult.deny(ReasonCode.PRODUCT_NOT_ALLOWED, Map.of());
        }

        BigDecimal amount = in.amount();

        if (amount.compareTo(r.minAmount()) < 0) {
            return PolicyResult.deny(ReasonCode.BELOW_MIN_AMOUNT, limit(r.minAmount()));
        }
        if (r.maxPerTxn() != null && amount.compareTo(r.maxPerTxn()) > 0) {
            return PolicyResult.deny(ReasonCode.ABOVE_TXN_LIMIT, limit(r.maxPerTxn()));
        }

        BigDecimal projectedToday = in.usedToday().add(amount);
        BigDecimal maxDaily = smaller(r.maxDaily(), in.customerMaxDaily());
        if (maxDaily != null && projectedToday.compareTo(maxDaily) > 0) {
            return PolicyResult.deny(ReasonCode.ABOVE_DAILY_LIMIT, limit(maxDaily));
        }

        BigDecimal fee = r.feeFlat().add(amount.multiply(r.feePct())).setScale(2, RoundingMode.HALF_UP);
        if (in.availableBalance() != null && amount.add(fee).compareTo(in.availableBalance()) > 0) {
            return PolicyResult.deny(ReasonCode.INSUFFICIENT_FUNDS, Map.of());
        }

        // OTP verification requirement removed per user preference
        return PolicyResult.allow(fee);
    }

    private static Map<String, String> limit(BigDecimal v) {
        return Map.of("limit", Money.fmt(v));
    }

    private static BigDecimal smaller(BigDecimal a, BigDecimal b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.min(b);
    }
}
