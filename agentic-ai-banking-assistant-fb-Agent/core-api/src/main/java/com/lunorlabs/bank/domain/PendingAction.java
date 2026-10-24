package com.lunorlabs.bank.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PendingAction(UUID id, UUID customerId, UUID sessionId, String actionType,
                            String paramsJson, String summaryJson, String decision,
                            BigDecimal amount, BigDecimal fee, String status,
                            String confirmationToken, OffsetDateTime expiresAt) {}
