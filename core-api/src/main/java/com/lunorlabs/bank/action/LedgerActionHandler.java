package com.lunorlabs.bank.action;

import com.lunorlabs.bank.domain.PendingAction;
import com.lunorlabs.bank.ledger.LedgerService;
import com.lunorlabs.bank.ledger.PostResult;

/** Base for handlers that move money. Non-money actions (card block, ...) implement execute themselves. */
public abstract class LedgerActionHandler implements ActionHandler {
    private final LedgerService ledger;

    protected LedgerActionHandler(LedgerService ledger) {
        this.ledger = ledger;
    }

    @Override
    public PostResult execute(PendingAction pending, PrepareOutcome outcome, String idempotencyKey) {
        return ledger.post(pending, outcome.plan(), outcome.summary(), idempotencyKey);
    }
}
