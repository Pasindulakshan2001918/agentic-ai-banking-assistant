package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.domain.Account;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class AccountRepo {
    private static final RowMapper<Account> MAP = (rs, i) -> new Account(
            rs.getObject("id", UUID.class), rs.getObject("customer_id", UUID.class),
            rs.getString("account_no"), rs.getString("product_type"), rs.getString("nickname"),
            rs.getString("currency").trim(), rs.getBigDecimal("balance"), rs.getString("status"));

    private final JdbcClient jdbc;

    public AccountRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    public List<Account> findByCustomer(UUID customerId) {
        return jdbc.sql("SELECT * FROM accounts WHERE customer_id = :c AND status <> 'CLOSED' ORDER BY account_no")
                .param("c", customerId).query(MAP).list();
    }

    public Optional<Account> findOwned(UUID id, UUID customerId) {
        return jdbc.sql("SELECT * FROM accounts WHERE id = :id AND customer_id = :c")
                .param("id", id).param("c", customerId).query(MAP).optional();
    }

    public Optional<Account> findById(UUID id) {
        if (id == null) return Optional.empty();
        return jdbc.sql("SELECT * FROM accounts WHERE id = :id AND status <> 'CLOSED'")
                .param("id", id).query(MAP).optional();
    }

    public Optional<Account> findByAccountNo(String accountNo) {
        if (accountNo == null || accountNo.isBlank()) return Optional.empty();
        return jdbc.sql("SELECT * FROM accounts WHERE account_no = :no AND status = 'ACTIVE'")
                .param("no", accountNo.trim()).query(MAP).optional();
    }

    /** Atomic conditional debit: returns false (and changes nothing) if funds are insufficient. */
    public boolean debit(UUID accountId, BigDecimal amount) {
        return jdbc.sql("UPDATE accounts SET balance = balance - :a WHERE id = :id AND status = 'ACTIVE' AND balance >= :a")
                .param("a", amount).param("id", accountId).update() == 1;
    }

    public void credit(UUID accountId, BigDecimal amount) {
        jdbc.sql("UPDATE accounts SET balance = balance + :a WHERE id = :id AND status = 'ACTIVE'")
                .param("a", amount).param("id", accountId).update();
    }
}
