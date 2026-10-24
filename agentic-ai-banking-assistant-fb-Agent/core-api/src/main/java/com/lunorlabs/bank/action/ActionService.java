package com.lunorlabs.bank.action;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.common.Json;
import com.lunorlabs.bank.common.Lang;
import com.lunorlabs.bank.common.RequestMeta;
import com.lunorlabs.bank.config.BankProperties;
import com.lunorlabs.bank.domain.PendingAction;
import com.lunorlabs.bank.i18n.MessageService;
import com.lunorlabs.bank.ledger.LedgerService;
import com.lunorlabs.bank.ledger.PostResult;
import com.lunorlabs.bank.otp.OtpService;
import com.lunorlabs.bank.policy.Decision;
import com.lunorlabs.bank.policy.PolicyResult;
import com.lunorlabs.bank.policy.ReasonCode;
import com.lunorlabs.bank.policy.ReasonDetail;
import com.lunorlabs.bank.repo.AuditEvent;
import com.lunorlabs.bank.repo.AuditRepo;
import com.lunorlabs.bank.repo.PendingActionRepo;
import com.lunorlabs.bank.repo.SessionRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** prepare -> (OTP) -> confirm. Orchestration only; rules live in handlers/PolicyEngine, money in LedgerService. */
@Service
public class ActionService {
    private static final SecureRandom RNG = new SecureRandom();

    private final ActionRegistry registry;
    private final PendingActionRepo pendings;
    private final SessionRepo sessions;
    private final OtpService otp;
    private final LedgerService ledger;
    private final AuditRepo audit;
    private final MessageService messages;
    private final BankProperties props;
    private final ObjectMapper om;

    public ActionService(ActionRegistry registry, PendingActionRepo pendings, SessionRepo sessions, OtpService otp,
                         LedgerService ledger, AuditRepo audit, MessageService messages, BankProperties props,
                         ObjectMapper om) {
        this.registry = registry;
        this.pendings = pendings;
        this.sessions = sessions;
        this.otp = otp;
        this.ledger = ledger;
        this.audit = audit;
        this.messages = messages;
        this.props = props;
        this.om = om;
    }

    // ------------------------------------------------------------------ prepare
    public PrepareResponse prepare(RequestMeta meta, String actionRaw, JsonNode params) {
        ActionType type = ActionType.parse(actionRaw);
        ActionHandler handler = registry.get(type);
        UUID cust = meta.customerId();
        Lang lang = meta.lang();
        UUID sid = (meta.sessionId() != null && sessions.touch(meta.sessionId(), cust, lang)) ? meta.sessionId() : null;
        JsonNode in = (params == null || params.isNull()) ? om.createObjectNode() : params;

        PrepareOutcome o = handler.prepare(new PrepareContext(cust, sid, lang), in);

        if (o.decision() == Decision.NEEDS_INFO) {
            audit(meta, "POLICY", "NEEDS_INFO", List.of(), null, null,
                    "missing=" + o.missing().stream().map(MissingField::field).toList(), type);
            return PrepareResponse.needsInfo(type, o.missing());
        }

        List<String> codes = o.policy().reasons().stream().map(r -> r.code().name()).toList();
        String explanation = explain(o.policy(), lang);

        if (o.decision() == Decision.DENY) {
            audit(meta, "POLICY", "DENY", codes, explanation, null, "denied", type);
            return PrepareResponse.denied(type, codes, explanation);
        }

        boolean stepUp = false; // OTP requirement removed per user preference
        String token = HexFormat.of().formatHex(randomBytes(32));
        OffsetDateTime expires = OffsetDateTime.now().plusMinutes(props.action().ttlMinutes());
        UUID id = pendings.insert(cust, sid, type.name(), Json.write(om, o.params()), Json.write(om, o.summary()),
                o.decision().name(), codes, o.plan().amount(), o.plan().fee(),
                stepUp ? "AWAITING_OTP" : "PREPARED", token, expires);
        String devOtp = stepUp ? otp.issue(id, cust) : null;

        audit(meta, "POLICY", o.decision().name(), codes, explanation, id, "prepared", type);
        return new PrepareResponse(o.decision(), type.name(), id, token, expires,
                messages.localize(o.summary(), lang), o.plan().amount(), o.plan().fee(), stepUp,
                codes, explanation, List.of(), devOtp);
    }

    // ------------------------------------------------------------------ confirm
    public Map<String, Object> confirm(RequestMeta meta, UUID pendingId, String token, String otpCode, String idemKey) {
        Lang lang = meta.lang();
        PendingAction p = load(pendingId, meta);
        ActionType type = ActionType.parse(p.actionType());
        ActionHandler handler = registry.get(type);

        // idempotent replay
        if ("COMPLETED".equals(p.status())) {
            PostResult r = ledger.findByPending(p.id())
                    .orElseThrow(() -> new IllegalStateException("Completed action has no transaction"));
            return completed(lang, p, r, type);
        }
        if (List.of("CANCELLED", "EXPIRED", "FAILED", "DENIED").contains(p.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_ALREADY_DONE", "This action is no longer pending.");
        }
        if (OffsetDateTime.now().isAfter(p.expiresAt())) {
            pendings.setStatus(p.id(), "EXPIRED");
            throw new ApiException(HttpStatus.GONE, "ACTION_EXPIRED", "This request has expired. Please start again.");
        }
        if (token == null || !MessageDigest.isEqual(
                token.getBytes(StandardCharsets.UTF_8), p.confirmationToken().getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Invalid confirmation token.");
        }

        // Re-run every rule: balance, limits or account state may have changed since prepare.
        PrepareOutcome o = handler.prepare(new PrepareContext(meta.customerId(), p.sessionId(), lang),
                Json.read(om, p.paramsJson()));
        if (o.decision() == Decision.DENY || o.decision() == Decision.NEEDS_INFO) {
            pendings.setStatus(p.id(), "FAILED");
            List<String> codes = o.policy() == null ? List.of()
                    : o.policy().reasons().stream().map(r -> r.code().name()).toList();
            String why = o.policy() == null ? messages.text("RECHECK_DENIED", lang, Map.of()) : explain(o.policy(), lang);
            audit(meta, "EXECUTION", "DENY", codes, why, p.id(), "denied_on_recheck", type);
            Map<String, Object> body = body("DENIED_ON_RECHECK", why);
            body.put("reasonCodes", codes);
            return body;
        }

        boolean needOtp = false; // OTP requirement removed per user preference
        if (needOtp) {
            if (otpCode == null || otpCode.isBlank() || !otp.hasChallenge(p.id())) {
                String devOtp = otp.hasChallenge(p.id()) ? null : otp.issue(p.id(), meta.customerId());
                return otpBody("OTP_REQUIRED", lang, devOtp);
            }
            OtpService.Result r = otp.verify(p.id(), otpCode.trim());
            switch (r.kind()) {
                case INVALID -> {
                    audit(meta, "OTP", "DENY", List.of("OTP_INVALID"), null, p.id(), "otp_invalid", type);
                    Map<String, Object> b = body("OTP_INVALID", messages.text("OTP_INVALID", lang, Map.of()));
                    b.put("attemptsLeft", r.attemptsLeft());
                    return b;
                }
                case EXPIRED -> {
                    audit(meta, "OTP", "DENY", List.of("OTP_EXPIRED"), null, p.id(), "otp_expired", type);
                    Map<String, Object> b = body("OTP_EXPIRED", messages.text("OTP_EXPIRED", lang, Map.of()));
                    b.put("canResend", true);
                    return b;
                }
                case LOCKED -> {
                    pendings.setStatus(p.id(), "CANCELLED");
                    audit(meta, "OTP", "DENY", List.of("OTP_LOCKED"), null, p.id(), "otp_locked", type);
                    return body("OTP_LOCKED", messages.text("OTP_LOCKED", lang, Map.of()));
                }
                case NO_CHALLENGE -> {
                    return otpBody("OTP_REQUIRED", lang, otp.issue(p.id(), meta.customerId()));
                }
                case VERIFIED -> audit(meta, "OTP", "ALLOW", List.of(), null, p.id(), "otp_verified", type);
            }
        }

        try {
            PostResult res = handler.execute(p, o, idemKey);
            audit(meta, "EXECUTION", "ALLOW", List.of(), null, p.id(), "COMPLETED " + res.txnRef(), type);
            return completed(lang, p, res, type);
        } catch (InsufficientFundsException e) {
            String why = messages.text(ReasonCode.INSUFFICIENT_FUNDS.name(), lang, Map.of());
            audit(meta, "EXECUTION", "DENY", List.of("INSUFFICIENT_FUNDS"), why, p.id(), "insufficient_funds", type);
            Map<String, Object> body = body("DENIED_ON_RECHECK", why);
            body.put("reasonCodes", List.of("INSUFFICIENT_FUNDS"));
            return body;
        }
    }

    // ------------------------------------------------------------------ resend / cancel
    public Map<String, Object> resendOtp(RequestMeta meta, UUID pendingId) {
        PendingAction p = load(pendingId, meta);
        if (!"AWAITING_OTP".equals(p.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_ALREADY_DONE", "No OTP is pending for this action.");
        }
        if (OffsetDateTime.now().isAfter(p.expiresAt())) {
            pendings.setStatus(p.id(), "EXPIRED");
            throw new ApiException(HttpStatus.GONE, "ACTION_EXPIRED", "This request has expired. Please start again.");
        }
        String devOtp = otp.issue(p.id(), meta.customerId());
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("sent", true);
        m.put("devOtp", devOtp);
        return m;
    }

    public Map<String, Object> cancel(RequestMeta meta, UUID pendingId) {
        PendingAction p = load(pendingId, meta);
        if (!List.of("PREPARED", "AWAITING_OTP").contains(p.status())) {
            throw new ApiException(HttpStatus.CONFLICT, "ACTION_ALREADY_DONE", "This action is no longer pending.");
        }
        pendings.setStatus(p.id(), "CANCELLED");
        audit(meta, "CONFIRMATION", "DENY", List.of(), null, p.id(), "cancelled_by_customer", ActionType.parse(p.actionType()));
        return body("CANCELLED", messages.text("ACTION_CANCELLED", meta.lang(), Map.of()));
    }

    // ------------------------------------------------------------------ helpers
    private PendingAction load(UUID id, RequestMeta meta) {
        return pendings.findOwned(id, meta.customerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Action not found"));
    }

    private String explain(PolicyResult r, Lang lang) {
        if (r.decision() == Decision.ALLOW) return null;
        return r.reasons().stream()
                .filter(d -> d.code() != ReasonCode.OK)
                .map((ReasonDetail d) -> messages.text(d.code().name(), lang, d.params()))
                .collect(Collectors.joining(" "));
    }

    private Map<String, Object> body(String status, String message) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("status", status);
        m.put("message", message);
        return m;
    }

    private Map<String, Object> otpBody(String status, Lang lang, String devOtp) {
        Map<String, Object> m = body(status, messages.text("OTP_REQUIRED", lang, Map.of()));
        if (devOtp != null) m.put("devOtp", devOtp);
        return m;
    }

    private Map<String, Object> completed(Lang lang, PendingAction p, PostResult r, ActionType type) {
        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("txnRef", r.txnRef());
        tx.put("actionType", type.name());
        tx.put("amount", p.amount());
        tx.put("fee", p.fee());
        tx.put("completedAt", r.completedAt());
        Map<String, Object> m = body("COMPLETED", messages.text(type.doneMessageKey(), lang, Map.of()));
        m.put("transaction", tx);
        m.put("receipt", Map.of("url", "/api/v1/receipts/" + r.txnRef()));
        return m;
    }

    private void audit(RequestMeta meta, String stage, String decision, List<String> codes, String explanation,
                       UUID pendingId, String outcome, ActionType type) {
        AuditEvent e = AuditEvent.of(stage);
        e.correlationId = meta.correlationId();
        e.sessionId = meta.sessionId();
        e.customerId = meta.customerId();
        e.language = meta.lang().name();
        e.toolName = type.name();
        e.decision = decision;
        e.reasonCodes = codes;
        e.explanation = explanation;
        e.pendingActionId = pendingId;
        e.outcome = outcome;
        audit.insert(e);
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RNG.nextBytes(b);
        return b;
    }
}
