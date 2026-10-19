package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.domain.PendingAction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PendingActionRepo {
    private static final RowMapper<PendingAction> MAP = (rs, i) -> new PendingAction(
            rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
            rs.getObject("session_id", UUID.class), rs.getString("action_type"),
            rs.getString("params_json"), rs.getString("summary_json"), rs.getString("decision"),
            rs.getBigDecimal("amount"), rs.getBigDecimal("fee"), rs.getString("status"),
            rs.getString("confirmation_token"), rs.getObject("expires_at", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public PendingActionRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    public UUID insert(UUID customerId, UUID sessionId, String actionType, String paramsJson, String summaryJson,
                       String decision, List<String> reasonCodes, BigDecimal amount, BigDecimal fee,
                       String status, String token, OffsetDateTime expiresAt) {
        return jdbc.sql("""
                INSERT INTO pending_actions
                  (customer_id, session_id, action_type, params, summary, decision, reason_codes,
                   amount, fee, status, confirmation_token, expires_at)
                VALUES (:c, :s, :t, CAST(:params AS jsonb), CAST(:summary AS jsonb), :d,
                        string_to_array(:codes, ','), :amt, :fee, :st, :tok, :exp)
                RETURNING id
                """)
                .param("c", customerId).param("s", sessionId).param("t", actionType)
                .param("params", paramsJson).param("summary", summaryJson).param("d", decision)
                .param("codes", String.join(",", reasonCodes))
                .param("amt", amount).param("fee", fee).param("st", status)
                .param("tok", token).param("exp", expiresAt)
                .query(UUID.class).single();
    }

    public Optional<PendingAction> findOwned(UUID id, UUID customerId) {
        return jdbc.sql("""
                SELECT id, customer_id, session_id, action_type,
                       CAST(params AS text) AS params_json, CAST(summary AS text) AS summary_json,
                       decision, amount, fee, status, confirmation_token, expires_at
                FROM pending_actions WHERE id = :id AND customer_id = :c
                """)
                .param("id", id).param("c", customerId).query(MAP).optional();
    }

    public void setStatus(UUID id, String status) {
        jdbc.sql("UPDATE pending_actions SET status = :s WHERE id = :id AND status NOT IN ('COMPLETED')")
                .param("s", status).param("id", id).update();
    }
}
