package com.lunorlabs.bank.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.common.Json;
import com.lunorlabs.bank.common.RequestMeta;
import com.lunorlabs.bank.repo.AuditEvent;
import com.lunorlabs.bank.repo.AuditRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Python-side events. Java-owned stages (POLICY/OTP/EXECUTION/CONFIRMATION) cannot be forged from here. */
@RestController
@RequestMapping("/api/v1/internal")
public class AuditController {
    private static final Set<String> PYTHON_STAGES = Set.of("INPUT", "INTENT", "TOOL_CALL", "RESPONSE", "FALLBACK");

    public record AuditRequest(String stage, String redactedUtterance, String intent, Double intentConfidence,
                               JsonNode slots, String toolName, String decision, List<String> reasonCodes,
                               String explanation, UUID pendingActionId, String outcome, String modelUsed,
                               Integer latencyMs) {}

    private final AuditRepo audit;
    private final ObjectMapper om;

    public AuditController(AuditRepo audit, ObjectMapper om) {
        this.audit = audit;
        this.om = om;
    }

    @PostMapping("/audit")
    public Map<String, Object> audit(HttpServletRequest req, @RequestBody AuditRequest b) {
        if (!"mcp-agent".equals(req.getHeader("X-Client"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Not allowed");
        }
        if (b.stage() == null || !PYTHON_STAGES.contains(b.stage())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid stage");
        }
        RequestMeta meta = RequestMeta.from(req);
        AuditEvent e = AuditEvent.of(b.stage());
        e.correlationId = meta.correlationId();
        e.sessionId = meta.sessionId();
        e.customerId = meta.customerId();
        e.language = meta.lang().name();
        e.redactedUtterance = b.redactedUtterance();
        e.intent = b.intent();
        e.intentConfidence = b.intentConfidence();
        e.slotsJson = b.slots() == null ? null : Json.write(om, b.slots());
        e.toolName = b.toolName();
        e.decision = b.decision();
        e.reasonCodes = b.reasonCodes();
        e.explanation = b.explanation();
        e.pendingActionId = b.pendingActionId();
        e.outcome = b.outcome();
        e.modelUsed = b.modelUsed();
        e.latencyMs = b.latencyMs();
        audit.insert(e);
        return Map.of("recorded", true);
    }
}
