package com.lunorlabs.bank.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.lunorlabs.bank.domain.PendingAction;
import com.lunorlabs.bank.ledger.PostResult;

/**
 * Strategy per action type. To add a feature: implement this (usually by extending LedgerActionHandler),
 * annotate with @Component, add an ActionType constant, add messages. The engine, OTP and audit stay untouched.
 */
public interface ActionHandler {
    ActionType type();

    /**
     * Resolve + validate params (must accept its own normalized output, because confirm re-runs it),
     * then evaluate policy. Must not move money.
     */
    PrepareOutcome prepare(PrepareContext ctx, JsonNode params);

    /** Called by confirm after the re-check and OTP passed. */
    PostResult execute(PendingAction pending, PrepareOutcome outcome, String idempotencyKey);
}
