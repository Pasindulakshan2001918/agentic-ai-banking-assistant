package com.lunorlabs.bank.repo;

import com.lunorlabs.bank.domain.Customer;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class CustomerRepo {
    private static final RowMapper<Customer> MAP = (rs, i) -> new Customer(
            rs.getObject("id", UUID.class), rs.getString("cif"), rs.getString("full_name"),
            rs.getString("mobile"), rs.getString("email"), rs.getString("preferred_language").trim());

    private final JdbcClient jdbc;

    public CustomerRepo(JdbcClient jdbc) { this.jdbc = jdbc; }

    public Optional<Customer> findById(UUID id) {
        return jdbc.sql("SELECT * FROM customers WHERE id = :id AND status = 'ACTIVE'")
                .param("id", id).query(MAP).optional();
    }

    public Optional<Customer> findByCif(String cif) {
        return jdbc.sql("SELECT * FROM customers WHERE cif = :cif AND status = 'ACTIVE'")
                .param("cif", cif).query(MAP).optional();
    }
}
