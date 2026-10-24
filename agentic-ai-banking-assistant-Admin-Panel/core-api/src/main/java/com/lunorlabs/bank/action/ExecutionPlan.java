package com.lunorlabs.bank.action;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** What the ledger must do. creditAccountId is set only for transfers between the customer's own accounts. */
public record ExecutionPlan(UUID fromAccountId, UUID creditAccountId, BigDecimal amount, BigDecimal fee,
                            String remarks, Map<String, Object> counterparty, UUID registeredBillerId) {}
