
package com.agentic.service;

import com.agentic.entity.Transaction;

import java.time.Instant;

/**

* DOMAIN EVENT - Published when transaction completes

* Used for event-driven side effects (notifications, audits, etc.)

*/

public class TransactionCompletedEvent {

private final Transaction transaction;

private final Instant occurredAt;

public TransactionCompletedEvent(Transaction transaction) {

if (transaction == null) {

throw new IllegalArgumentException("Transaction cannot be null");

}

this.transaction = transaction;

this.occurredAt = Instant.now();

}

public Transaction getTransaction() {

return transaction;

}

public Instant getOccurredAt() {

return occurredAt;

}

@Override

public String toString() {

return "TransactionCompletedEvent{" +

"transactionId=" + transaction.getId() +

", amount=" + transaction.getAmount() +

", occurredAt=" + occurredAt +

'}';

}

}
