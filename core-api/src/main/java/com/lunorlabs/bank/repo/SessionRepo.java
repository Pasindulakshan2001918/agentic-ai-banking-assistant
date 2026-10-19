package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.common.Lang;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class SessionRepo {
    private final JdbcClient jdbc;

    public SessionRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Upserts the session. Returns false if the id already belongs to a different customer. */
    public boolean touch(UUID sessionId, UUID customerId, Lang lang) {
        return jdbc.sql("""
                INSERT INTO chat_sessions (id, customer_id, language) VALUES (:id, :c, :l)
                ON CONFLICT (id) DO UPDATE SET last_active_at = now()
                WHERE chat_sessions.customer_id = EXCLUDED.customer_id
                """)
                .param("id", sessionId).param("c", customerId).param("l", lang.name())
                .update() == 1;
    }
}
