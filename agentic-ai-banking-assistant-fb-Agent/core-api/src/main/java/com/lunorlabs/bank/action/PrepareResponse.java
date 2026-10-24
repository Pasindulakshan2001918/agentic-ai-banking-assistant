package com.lunorlabs.bank.action;

import com.lunorlabs.bank.policy.Decision;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Contract section 2. Null fields are serialized as null on purpose (stable shape for the agent). */
public record PrepareResponse(Decision decision, String actionType, UUID pendingActionId,
                              String confirmationToken, OffsetDateTime expiresAt, LocalizedSummary summary,
                              BigDecimal amount, BigDecimal fee, boolean otpRequired,
                              List<String> reasonCodes, String explanation,
                              List<MissingField> missing, String devOtp) {

    public static PrepareResponse needsInfo(ActionType type, List<MissingField> missing) {
        return new PrepareResponse(Decision.NEEDS_INFO, type.name(), null, null, null, null,
                null, null, false, List.of(), null, missing, null);
    }

    public static PrepareResponse denied(ActionType type, List<String> codes, String explanation) {
        return new PrepareResponse(Decision.DENY, type.name(), null, null, null, null,
                null, null, false, codes, explanation, List.of(), null);
    }
}
