package com.lunorlabs.bank.policy;

import com.lunorlabs.bank.domain.ProductRule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyEngineTest {
    private final PolicyEngine engine = new PolicyEngine();

    private static BigDecimal bd(String v) { return new BigDecimal(v); }

    /** Mirrors the seeded FUND_TRANSFER/INTERNAL row. */
    private static ProductRule rule(boolean enabled, String min, boolean alwaysOtp, String feeFlat) {
        return new ProductRule("FUND_TRANSFER", "ANY", "INTERNAL", enabled, bd(min), bd("1000000"), bd("2000000"),
                bd("25000"), bd("50000"), alwaysOtp, 24, bd(feeFlat), bd("0"));
    }

    private static ProductRule std() { return rule(true, "1", false, "0"); }

    private PolicyResult run(ProductRule r, String amount, String balance, String used, boolean newPayee, String custMax) {
        return engine.evaluate(new PolicyInput(r, bd(amount), bd(balance), true, newPayee, bd(used),
                custMax == null ? null : bd(custMax)));
    }

    @Test void smallTransferIsAllowed() {
        PolicyResult r = run(std(), "1000", "250000", "0", false, null);
        assertThat(r.decision()).isEqualTo(Decision.ALLOW);
        assertThat(r.codes()).containsExactly(ReasonCode.OK);
    }

    @Test void highValueNeedsOtp() {
        PolicyResult r = run(std(), "30000", "250000", "0", false, null);
        assertThat(r.decision()).isEqualTo(Decision.STEP_UP);
        assertThat(r.codes()).contains(ReasonCode.OTP_HIGH_VALUE);
        assertThat(r.reasons().get(0).params()).containsEntry("limit", "LKR 25,000.00");
    }

    @Test void dailyCumulativeNeedsOtpEvenWhenSingleAmountIsSmall() {
        PolicyResult r = run(std(), "20000", "250000", "40000", false, null);   // 60,000 > 50,000
        assertThat(r.decision()).isEqualTo(Decision.STEP_UP);
        assertThat(r.codes()).containsExactly(ReasonCode.OTP_DAILY_CUMULATIVE);
    }

    @Test void newPayeeNeedsOtp() {
        PolicyResult r = run(std(), "500", "250000", "0", true, null);
        assertThat(r.decision()).isEqualTo(Decision.STEP_UP);
        assertThat(r.codes()).containsExactly(ReasonCode.OTP_NEW_PAYEE);
    }

    @Test void mandatoryOtpRule() {
        PolicyResult r = run(rule(true, "1", true, "0"), "500", "250000", "0", false, null);
        assertThat(r.codes()).containsExactly(ReasonCode.OTP_MANDATORY_ACTION);
    }

    @Test void multipleStepUpReasonsAccumulate() {
        PolicyResult r = run(std(), "30000", "250000", "40000", true, null);
        assertThat(r.codes()).containsExactly(ReasonCode.OTP_HIGH_VALUE, ReasonCode.OTP_DAILY_CUMULATIVE,
                ReasonCode.OTP_NEW_PAYEE);
    }

    @Test void insufficientFundsIsDenied() {
        PolicyResult r = run(std(), "30000", "20000", "0", false, null);
        assertThat(r.decision()).isEqualTo(Decision.DENY);
        assertThat(r.codes()).containsExactly(ReasonCode.INSUFFICIENT_FUNDS);
    }

    @Test void feeCountsTowardBalance() {
        PolicyResult r = run(rule(true, "1", false, "25"), "1000", "1010", "0", false, null);   // 1,025 > 1,010
        assertThat(r.decision()).isEqualTo(Decision.DENY);
        assertThat(r.codes()).containsExactly(ReasonCode.INSUFFICIENT_FUNDS);
    }

    @Test void feeIsReportedOnAllow() {
        PolicyResult r = run(rule(true, "1", false, "25"), "1000", "250000", "0", false, null);
        assertThat(r.fee()).isEqualByComparingTo("25.00");
    }

    @Test void customerDailyOverrideIsEnforced() {
        PolicyResult r = run(std(), "12000", "250000", "0", false, "10000");
        assertThat(r.decision()).isEqualTo(Decision.DENY);
        assertThat(r.codes()).containsExactly(ReasonCode.ABOVE_DAILY_LIMIT);
        assertThat(r.reasons().get(0).params()).containsEntry("limit", "LKR 10,000.00");
    }

    @Test void productDailyCapIsEnforced() {
        PolicyResult r = run(std(), "100000", "9000000", "1950000", false, null);   // 2,050,000 > 2,000,000
        assertThat(r.codes()).containsExactly(ReasonCode.ABOVE_DAILY_LIMIT);
    }

    @Test void perTransactionCapIsEnforced() {
        PolicyResult r = run(std(), "1500000", "9000000", "0", false, null);
        assertThat(r.codes()).containsExactly(ReasonCode.ABOVE_TXN_LIMIT);
    }

    @Test void belowMinimumIsDenied() {
        PolicyResult r = run(rule(true, "50", false, "0"), "10", "250000", "0", false, null);
        assertThat(r.codes()).containsExactly(ReasonCode.BELOW_MIN_AMOUNT);
    }

    @Test void disabledOrMissingRuleIsDenied() {
        assertThat(run(rule(false, "1", false, "0"), "500", "250000", "0", false, null).codes())
                .containsExactly(ReasonCode.PRODUCT_NOT_ALLOWED);
        assertThat(run(null, "500", "250000", "0", false, null).codes())
                .containsExactly(ReasonCode.PRODUCT_NOT_ALLOWED);
    }

    @Test void inactiveAccountIsDeniedFirst() {
        PolicyResult r = engine.evaluate(new PolicyInput(std(), bd("500"), bd("250000"), false, false, bd("0"), null));
        assertThat(r.codes()).containsExactly(ReasonCode.ACCOUNT_INACTIVE);
    }

    @Test void denyWinsOverStepUp() {
        PolicyResult r = run(std(), "30000", "100", "0", true, null);   // would need OTP, but no money
        assertThat(r.decision()).isEqualTo(Decision.DENY);
    }
}
