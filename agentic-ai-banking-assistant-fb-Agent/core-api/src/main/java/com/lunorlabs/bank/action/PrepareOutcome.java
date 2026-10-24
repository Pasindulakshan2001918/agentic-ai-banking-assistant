package com.lunorlabs.bank.action;

import com.lunorlabs.bank.policy.Decision;
import com.lunorlabs.bank.policy.PolicyResult;

import java.util.List;
import java.util.Map;

/** Result of ActionHandler.prepare. Money is never moved while producing this. */
public record PrepareOutcome(Decision decision, List<MissingField> missing, PolicyResult policy,
                             Map<String, Object> params, Summary summary, ExecutionPlan plan) {

    public static PrepareOutcome needsInfo(List<MissingField> missing) {
        return new PrepareOutcome(Decision.NEEDS_INFO, missing, null, null, null, null);
    }

    public static PrepareOutcome denied(PolicyResult policy) {
        return new PrepareOutcome(Decision.DENY, List.of(), policy, null, null, null);
    }

    public static PrepareOutcome ready(PolicyResult policy, Map<String, Object> params, Summary summary,
                                       ExecutionPlan plan) {
        return new PrepareOutcome(policy.decision(), List.of(), policy, params, summary, plan);
    }
}
