package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.domain.Biller;
import com.lunorlabs.bank.domain.RegisteredBiller;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public class BillerRepo {
    private static final RowMapper<Biller> BILLER = (rs, i) -> new Biller(
            rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name_en"),
            rs.getString("name_si"), rs.getString("name_ta"), rs.getString("reference_label"),
            rs.getString("category"), rs.getDouble("score"));

    private static final RowMapper<RegisteredBiller> REGISTERED = (rs, i) -> new RegisteredBiller(
            rs.getObject("rb_id", UUID.class), rs.getString("reference_no"), rs.getString("nickname"),
            new Biller(rs.getObject("id", UUID.class), rs.getString("code"), rs.getString("name_en"),
                    rs.getString("name_si"), rs.getString("name_ta"), rs.getString("reference_label"),
                    rs.getString("category"), 1.0),
            rs.getBigDecimal("amount_due"), rs.getObject("due_date", LocalDate.class));

    private static final String REGISTERED_SQL = """
            SELECT rb.id AS rb_id, rb.reference_no, rb.nickname,
                   b.id, b.code, b.name_en, b.name_si, b.name_ta, b.reference_label, b.category,
                   d.amount_due, d.due_date
            FROM registered_billers rb
            JOIN billers b ON b.id = rb.biller_id
            LEFT JOIN LATERAL (
                SELECT amount_due, due_date FROM bill_dues
                WHERE registered_biller_id = rb.id AND status = 'UNPAID'
                ORDER BY due_date LIMIT 1) d ON true
            WHERE rb.customer_id = :c
            """;

    private final JdbcClient jdbc;

    public BillerRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    /** Alias match first, then substring, then trigram similarity. Best 5, ranked. */
    public List<Biller> search(String q) {
        String norm = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
        if (norm.isEmpty()) return List.of();
        return jdbc.sql("""
                SELECT id, code, name_en, name_si, name_ta, reference_label, category,
                       GREATEST(CASE WHEN :q = ANY(aliases) THEN 1.0::float8 ELSE 0.0::float8 END,
                                similarity(lower(name_en), :q)::float8,
                                similarity(lower(code), :q)::float8) AS score
                FROM billers
                WHERE active AND (
                      :q = ANY(aliases)
                   OR name_en ILIKE :like OR name_si ILIKE :like OR name_ta ILIKE :like
                   OR array_to_string(aliases, ' ') ILIKE :like
                   OR similarity(lower(name_en), :q) > 0.25)
                ORDER BY score DESC, name_en
                LIMIT 5
                """)
                .param("q", norm).param("like", "%" + norm + "%")
                .query(BILLER).list();
    }

    public Optional<Biller> findById(UUID id) {
        return jdbc.sql("SELECT id, code, name_en, name_si, name_ta, reference_label, category, 1.0::float8 AS score "
                        + "FROM billers WHERE id = :id AND active")
                .param("id", id).query(BILLER).optional();
    }

    public List<RegisteredBiller> registered(UUID customerId) {
        return jdbc.sql(REGISTERED_SQL + " ORDER BY b.name_en").param("c", customerId).query(REGISTERED).list();
    }

    public Optional<RegisteredBiller> findRegistered(UUID id, UUID customerId) {
        return jdbc.sql(REGISTERED_SQL + " AND rb.id = :id").param("c", customerId).param("id", id)
                .query(REGISTERED).optional();
    }
}
