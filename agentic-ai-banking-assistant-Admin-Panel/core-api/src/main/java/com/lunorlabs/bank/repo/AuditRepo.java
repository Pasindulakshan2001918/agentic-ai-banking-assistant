package com.lunorlabs.bank.repo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AuditRepo {
    private static final Logger log = LoggerFactory.getLogger(AuditRepo.class);
    private final JdbcClient jdbc;

    public AuditRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Append-only (a DB trigger blocks UPDATE/DELETE). Audit failure must never break a banking call. */
    public void insert(AuditEvent e) {
        try {
            jdbc.sql("""
                    INSERT INTO audit_log
                      (correlation_id, session_id, customer_id, channel, language, stage, redacted_utterance,
                       intent, intent_confidence, slots, tool_name, decision, reason_codes, explanation,
                       pending_action_id, outcome, model_used, latency_ms)
                    VALUES (:corr, :sess, :cust, :channel, :lang, :stage, :utt,
                            :intent, :conf, CAST(:slots AS jsonb), :tool, :decision, string_to_array(:codes, ','),
                            :expl, :pending, :outcome, :model, :latency)
                    """)
                    .param("corr", e.correlationId).param("sess", e.sessionId).param("cust", e.customerId)
                    .param("channel", e.channel).param("lang", e.language).param("stage", e.stage)
                    .param("utt", e.redactedUtterance).param("intent", e.intent).param("conf", e.intentConfidence)
                    .param("slots", e.slotsJson).param("tool", e.toolName).param("decision", e.decision)
                    .param("codes", e.reasonCodes == null ? null : String.join(",", e.reasonCodes))
                    .param("expl", e.explanation).param("pending", e.pendingActionId)
                    .param("outcome", e.outcome).param("model", e.modelUsed).param("latency", e.latencyMs)
                    .update();
        } catch (RuntimeException ex) {
            log.error("audit insert failed (stage={})", e.stage, ex);
        }
    }
}
