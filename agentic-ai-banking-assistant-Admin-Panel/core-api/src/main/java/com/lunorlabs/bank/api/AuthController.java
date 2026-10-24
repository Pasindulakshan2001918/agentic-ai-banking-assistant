package com.lunorlabs.bank.api;

import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.repo.CustomerRepo;
import com.lunorlabs.bank.security.JwtService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/** Demo login by CIF. Exists only when bank.dev-login-enabled=true (dev profile). */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "bank.dev-login-enabled", havingValue = "true")
public class AuthController {
    public record DevLoginRequest(String cif) {}

    private final CustomerRepo customers;
    private final JwtService jwt;
    private final JdbcClient jdbc;

    public AuthController(CustomerRepo customers, JwtService jwt, JdbcClient jdbc) {
        this.customers = customers;
        this.jwt = jwt;
        this.jdbc = jdbc;
    }

    @PostMapping("/dev-login")
    public Map<String, Object> devLogin(@RequestBody DevLoginRequest req) {
        var c = customers.findByCif(req.cif() == null ? "" : req.cif().trim())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Unknown CIF"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accessToken", jwt.issue(c.id()));
        m.put("expiresIn", jwt.ttlSeconds());
        m.put("customerId", c.id());
        m.put("preferredLanguage", c.preferredLanguage());
        return m;
    }

    @PostMapping("/dev-signup")
    public Map<String, Object> devSignup(@RequestBody Map<String, String> payload) {
        String cif = payload.get("cif");
        String name = payload.get("name");
        String mobile = payload.get("mobile");
        String lang = payload.getOrDefault("lang", "EN").toUpperCase();

        if (customers.findByCif(cif).isPresent()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "EXISTS", "CIF already exists");
        }

        UUID id = jdbc.sql("INSERT INTO customers (cif, full_name, mobile, preferred_language) VALUES (?, ?, ?, ?) RETURNING id")
                .param(1, cif).param(2, name).param(3, mobile).param(4, lang)
                .query(UUID.class).single();

        String accNo = "77" + System.currentTimeMillis();
        jdbc.sql("INSERT INTO accounts (customer_id, account_no, product_type, nickname, balance) VALUES (?, ?, 'SAVINGS', 'Main Savings', 100000)")
                .param(1, id).param(2, accNo).update();

        jdbc.sql("INSERT INTO loans (customer_id, loan_no, product_type, principal, outstanding) VALUES (?, ?, 'LOAN', 500000, 250000)")
                .param(1, id).param(2, "LN" + System.currentTimeMillis()).update();

        jdbc.sql("INSERT INTO cards (customer_id, card_type, network, card_last4, credit_limit, outstanding_balance) VALUES (?, 'CREDIT', 'VISA', '1234', 100000, 5000)")
                .param(1, id).update();

        return devLogin(new DevLoginRequest(cif));
    }
}
