package com.lunorlabs.bank.repo;

import java.util.List;
import java.util.UUID;

/** Plain holder for one audit_log row. Public fields on purpose: it is a dumb DTO. */
public class AuditEvent {
    public String stage;
    public UUID correlationId;
    public UUID sessionId;
    public UUID customerId;
    public String channel = "AI_ASSISTANT";
    public String language;
    public String redactedUtterance;
    public String intent;
    public Double intentConfidence;
    public String slotsJson;
    public String toolName;
    public String decision;
    public List<String> reasonCodes;
    public String explanation;
    public UUID pendingActionId;
    public String outcome;
    public String modelUsed;
    public Integer latencyMs;

    public static AuditEvent of(String stage) {
        AuditEvent e = new AuditEvent();
        e.stage = stage;
        return e;
    }
}
