package com.lunorlabs.bank.policy;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PolicyResult(Decision decision, List<ReasonDetail> reasons, BigDecimal fee) {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public static PolicyResult deny(ReasonCode code, Map<String, String> params) {
        return new PolicyResult(Decision.DENY, List.of(new ReasonDetail(code, params)), ZERO);
    }

    public static PolicyResult allow(BigDecimal fee) {
        return new PolicyResult(Decision.ALLOW, List.of(new ReasonDetail(ReasonCode.OK, Map.of())), fee);
    }

    public static PolicyResult stepUp(List<ReasonDetail> reasons, BigDecimal fee) {
        return new PolicyResult(Decision.STEP_UP, List.copyOf(reasons), fee);
    }

    public List<ReasonCode> codes() {
        return reasons.stream().map(ReasonDetail::code).toList();
    }
}
