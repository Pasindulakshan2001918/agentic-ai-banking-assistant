package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.domain.Payee;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class PayeeRepo {
    private static final RowMapper<Payee> MAP = (rs, i) -> new Payee(
            rs.getObject("id", UUID.class), rs.getString("nickname"), rs.getString("beneficiary_name"),
            rs.getString("bank_code"), rs.getString("bank_name"), rs.getString("account_no"),
            rs.getObject("added_at", OffsetDateTime.class));

    private final JdbcClient jdbc;

    public PayeeRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Payee> search(UUID customerId, String q) {
        if (q == null || q.isBlank()) {
            return jdbc.sql("SELECT * FROM payees WHERE customer_id = :c ORDER BY nickname")
                    .param("c", customerId).query(MAP).list();
        }
        return jdbc.sql("SELECT * FROM payees WHERE customer_id = :c "
                        + "AND (nickname ILIKE :q OR beneficiary_name ILIKE :q) ORDER BY nickname")
                .param("c", customerId).param("q", "%" + q.trim() + "%").query(MAP).list();
    }

    public Optional<Payee> findOwned(UUID id, UUID customerId) {
        return jdbc.sql("SELECT * FROM payees WHERE id = :id AND customer_id = :c")
                .param("id", id).param("c", customerId).query(MAP).optional();
    }
}
