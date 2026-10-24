package com.lunorlabs.bank.api;

import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.*;

/**
 * Real password-based authentication for mobile customers.
 *
 * POST /api/v1/auth/login    – login with username + password
 * POST /api/v1/auth/register – link username/password to an admin-created account
 */
@RestController
@RequestMapping("/api/v1/auth")
public class UserAuthController {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private final JdbcClient jdbc;
    private final JwtService jwt;

    public UserAuthController(JdbcClient jdbc, JwtService jwt) {
        this.jdbc = jdbc;
        this.jwt = jwt;
    }

    // ── Login ──────────────────────────────────────────────────────────────────

    public record LoginRequest(String username, String password) {}

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody LoginRequest req) {
        if (req.username() == null || req.username().isBlank() ||
            req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Username and password are required.");
        }

        // Look up credential record
        List<Map<String, Object>> rows = jdbc.sql(
            "SELECT uc.id, uc.customer_id, uc.password_hash, c.full_name, c.preferred_language " +
            "FROM user_credentials uc " +
            "JOIN customers c ON c.id = uc.customer_id " +
            "WHERE lower(uc.username) = lower(?)")
            .param(1, req.username().trim())
            .query().listOfRows();

        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.");
        }

        Map<String, Object> record = rows.get(0);
        String storedHash = (String) record.get("password_hash");
        if (!BCRYPT.matches(req.password(), storedHash)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid username or password.");
        }

        UUID customerId = UUID.fromString(record.get("customer_id").toString());

        // Update last login timestamp
        jdbc.sql("UPDATE user_credentials SET last_login_at = now() WHERE customer_id = ?")
            .param(1, customerId).update();

        return buildTokenResponse(customerId, (String) record.get("full_name"),
                                  (String) record.get("preferred_language"));
    }

    // ── Register ───────────────────────────────────────────────────────────────

    public record RegisterRequest(String accountNo, String username, String password) {}

    @PostMapping("/register")
    public Map<String, Object> register(@RequestBody RegisterRequest req) {
        if (req.accountNo() == null || req.accountNo().isBlank() ||
            req.username()  == null || req.username().isBlank()  ||
            req.password()  == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS",
                "Account number, username and password are all required.");
        }

        if (req.password().length() < 8) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "WEAK_PASSWORD",
                "Password must be at least 8 characters.");
        }

        // Check account exists (admin must have created it first)
        List<Map<String, Object>> accountRows = jdbc.sql(
            "SELECT a.customer_id, c.full_name, c.preferred_language " +
            "FROM accounts a JOIN customers c ON c.id = a.customer_id " +
            "WHERE a.account_no = ? AND a.status = 'ACTIVE'")
            .param(1, req.accountNo().trim())
            .query().listOfRows();

        if (accountRows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                "No active account found for that account number. Please contact your bank.");
        }

        Map<String, Object> account = accountRows.get(0);
        UUID customerId = UUID.fromString(account.get("customer_id").toString());

        // Check if this customer already registered
        boolean alreadyRegistered = jdbc.sql(
            "SELECT 1 FROM user_credentials WHERE customer_id = ?")
            .param(1, customerId)
            .query(Integer.class).list().size() > 0;

        if (alreadyRegistered) {
            throw new ApiException(HttpStatus.CONFLICT, "ALREADY_REGISTERED",
                "This account already has a registered user. Please log in instead.");
        }

        // Check username uniqueness
        boolean usernameTaken = jdbc.sql(
            "SELECT 1 FROM user_credentials WHERE lower(username) = lower(?)")
            .param(1, req.username().trim())
            .query(Integer.class).list().size() > 0;

        if (usernameTaken) {
            throw new ApiException(HttpStatus.CONFLICT, "USERNAME_TAKEN",
                "That username is already taken. Please choose another.");
        }

        String hash = BCRYPT.encode(req.password());
        jdbc.sql(
            "INSERT INTO user_credentials (customer_id, account_no, username, password_hash) " +
            "VALUES (?, ?, ?, ?)")
            .param(1, customerId)
            .param(2, req.accountNo().trim())
            .param(3, req.username().trim())
            .param(4, hash)
            .update();

        return buildTokenResponse(customerId, (String) account.get("full_name"),
                                  (String) account.get("preferred_language"));
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private Map<String, Object> buildTokenResponse(UUID customerId, String fullName, String preferredLanguage) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accessToken", jwt.issue(customerId));
        m.put("expiresIn", jwt.ttlSeconds());
        m.put("customerId", customerId);
        m.put("fullName", fullName);
        m.put("preferredLanguage", preferredLanguage != null ? preferredLanguage : "EN");
        return m;
    }
}
