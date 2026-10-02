package com.lunorlabs.bank.ledger;

import java.time.OffsetDateTime;

public record PostResult(String txnRef, OffsetDateTime completedAt, boolean alreadyDone) {}
