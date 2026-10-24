package com.lunorlabs.bank.api;

import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private static final List<String> PERMISSIONS = List.of("*");
    private static final List<String> CAPABILITIES = List.of(
            "dashboard", "users", "transactions", "bills", "security", "ai", "audit", "reports", "administrators", "settings");
    private static final int MAX_PAGE_SIZE = 100;

    private final JdbcTemplate jdbc;
    private final JwtService jwt;
    private final PasswordEncoder passwordEncoder;
    private final String adminEmail;
    private final String adminPassword;
    private final String adminName;
    private final String adminPasswordHash;
    private static final String ADMIN_EMAIL_PATTERN = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
    private final ConcurrentHashMap<String, LoginAttempts> loginAttempts = new ConcurrentHashMap<>();

    public AdminController(
            JdbcTemplate jdbc,
            JwtService jwt,
            PasswordEncoder passwordEncoder,
            @Value("${bank.admin.email:}") String adminEmail,
            @Value("${bank.admin.password:}") String adminPassword,
            @Value("${bank.admin.name:System Administrator}") String adminName) {
        this.jdbc = jdbc;
        this.jwt = jwt;
        this.passwordEncoder = passwordEncoder;
        this.adminEmail = adminEmail.trim().toLowerCase();
        this.adminPassword = adminPassword;
        this.adminName = adminName.isBlank() ? "System Administrator" : adminName.trim();
        this.adminPasswordHash = adminPassword.length() >= 14 ? passwordEncoder.encode(adminPassword) : "";
    }

    public record LoginRequest(String email, String password) {}
    public record CreateAdminRequest(String fullName, String email, String password) {}
    public record FirstLoginCredentialsRequest(String email, String password, String confirmPassword) {}
    public record UpdateCredentialsRequest(String currentPassword, String email, String password, String confirmPassword) {}
    public record CreateUserRequest(String cif, String fullName, String mobile, String email,
                                    String preferredLanguage, String reason) {}
    public record StatusRequest(String status, String reason) {}
    public record InvestigationRequest(String status, String notes, String assignedTo) {}
    private static final class LoginAttempts {
        private long windowStartedAt;
        private int count;
    }

    @PostMapping("/auth/login")
    @Transactional
    public Map<String, Object> login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
        String ip = servletRequest.getRemoteAddr();
        LoginAttempts attempts = loginAttempts.computeIfAbsent(ip, ignored -> new LoginAttempts());
        synchronized (attempts) {
            long now = System.currentTimeMillis();
            if (now - attempts.windowStartedAt > 15 * 60 * 1000L) {
                attempts.windowStartedAt = now;
                attempts.count = 0;
            }
            if (attempts.count >= 5) {
                throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Too many sign-in attempts. Try again later");
            }
            attempts.count++;
        }
        String email = request.email() == null ? "" : request.email().trim().toLowerCase();
        String password = request.password() == null ? "" : request.password();
        UUID administratorId = null;
        String role;
        Map<String, Object> account = null;
        if (configuredAdmin() && adminEmail.equals(email)) {
            if (!passwordEncoder.matches(password, adminPasswordHash)) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid administrator email or password");
            }
            role = "SUPER_ADMIN";
        } else {
            List<Map<String, Object>> accounts = jdbc.queryForList("""
                    SELECT id, email, full_name, password_hash, role, status, must_change_credentials, last_login_at
                    FROM admin_console.administrators WHERE lower(email) = ?
                    """, email);
            if (accounts.isEmpty()) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid administrator email or password");
            }
            account = accounts.getFirst();
            if (!"ACTIVE".equals(account.get("status"))
                    || !passwordEncoder.matches(password, String.valueOf(account.get("password_hash")))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid administrator email or password");
            }
            administratorId = (UUID) account.get("id");
            role = String.valueOf(account.get("role"));
        }
        if (!List.of("ADMIN", "SUPER_ADMIN").contains(role)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Invalid administrator email or password");
        }
        loginAttempts.remove(ip);
        UUID sessionId = UUID.randomUUID();
        jdbc.update("DELETE FROM admin_console.admin_sessions WHERE expires_at <= now()");
        jdbc.update("""
                INSERT INTO admin_console.admin_sessions (id, email, role, administrator_id, expires_at)
                VALUES (?, ?, ?, ?, now() + (? * interval '1 second'))
                """, sessionId, email, role, administratorId, jwt.ttlSeconds());
        if (administratorId == null) {
            recordEvent(email, "LOGIN", "ADMINISTRATOR", sessionId.toString(), Map.of("source", "admin-console"));
        } else {
            jdbc.update("UPDATE admin_console.administrators SET last_login_at = now() WHERE id = ?", administratorId);
            recordEvent(email, "LOGIN", "ADMINISTRATOR", sessionId.toString(), Map.of("source", "admin-console"));
            account = jdbc.queryForMap("""
                    SELECT id, email, full_name, role, status, must_change_credentials, last_login_at
                    FROM admin_console.administrators WHERE id = ?
                    """, administratorId);
        }
        Map<String, Object> profile = administratorId == null
                ? administrator(sessionId.toString())
                : administratorProfile(account);
        return Map.of("accessToken", jwt.issueAdmin(email, sessionId), "administrator", profile,
                "expiresIn", jwt.ttlSeconds());
    }

    @PostMapping("/auth/first-login-credentials")
    @Transactional
    public Map<String, Object> changeInitialCredentials(@RequestBody FirstLoginCredentialsRequest request,
                                                         HttpServletRequest servletRequest,
                                                         Authentication authentication) {
        String email = requiredEmail(request.email(), "New username");
        String password = requiredPassword(request.password(), "New password");
        if (!password.equals(request.confirmPassword())) throw badRequest("Password and confirmation do not match");
        if (email.equals(adminEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already assigned to the Super Admin");
        }
        UUID sessionId = jwt.parseAdmin(bearerToken(servletRequest)).sessionId();
        List<Map<String, Object>> accounts = jdbc.queryForList("""
                SELECT a.id, a.email FROM admin_console.administrators a
                JOIN admin_console.admin_sessions s ON s.administrator_id = a.id
                WHERE s.id = ? AND s.email = ? AND s.role = 'ADMIN'
                  AND a.status = 'ACTIVE' AND a.must_change_credentials = true
                """, sessionId, authentication.getName());
        if (accounts.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "This account does not require initial credential changes");
        }
        Map<String, Object> account = accounts.getFirst();
        UUID administratorId = (UUID) account.get("id");
        if (email.equalsIgnoreCase(String.valueOf(account.get("email")))) {
            throw badRequest("Choose a new username different from the initial username");
        }
        ensureUsernameAvailable(email, administratorId);
        try {
            jdbc.update("""
                    UPDATE admin_console.administrators
                    SET email = ?, password_hash = ?, must_change_credentials = false
                    WHERE id = ?
                    """, email, passwordEncoder.encode(password), administratorId);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already in use");
        }
        jdbc.update("UPDATE admin_console.admin_sessions SET email = ? WHERE id = ?", email, sessionId);
        jdbc.update("DELETE FROM admin_console.admin_sessions WHERE administrator_id = ? AND id <> ?", administratorId, sessionId);
        recordEvent(authentication.getName(), "CHANGE_INITIAL_CREDENTIALS", "ADMINISTRATOR",
                administratorId.toString(), Map.of("usernameChanged", true));
        return credentialChangeResponse(administratorId, email, sessionId);
    }

    @PatchMapping("/auth/credentials")
    @Transactional
    public Map<String, Object> updateCredentials(@RequestBody UpdateCredentialsRequest request,
                                                  HttpServletRequest servletRequest,
                                                  Authentication authentication) {
        UUID sessionId = jwt.parseAdmin(bearerToken(servletRequest)).sessionId();
        List<Map<String, Object>> accounts = jdbc.queryForList("""
                SELECT a.id, a.email, a.full_name, a.password_hash, a.role, a.status, a.last_login_at
                FROM admin_console.administrators a
                JOIN admin_console.admin_sessions s ON s.administrator_id = a.id
                WHERE s.id = ? AND s.email = ? AND s.role = 'ADMIN'
                  AND a.status = 'ACTIVE' AND a.must_change_credentials = false
                """, sessionId, authentication.getName());
        if (accounts.isEmpty()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only registered Admin accounts can change credentials here");
        }
        Map<String, Object> account = accounts.getFirst();
        UUID administratorId = (UUID) account.get("id");
        String email = request.email() == null || request.email().isBlank()
                ? String.valueOf(account.get("email")) : requiredEmail(request.email(), "Username");
        String password = request.password() == null ? "" : request.password();
        if (email.equals(String.valueOf(account.get("email"))) && password.isBlank()) {
            throw badRequest("Enter a new username or password");
        }
        if (!password.isBlank()) {
            if (!passwordEncoder.matches(request.currentPassword() == null ? "" : request.currentPassword(),
                    String.valueOf(account.get("password_hash")))) {
                throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Current password is incorrect");
            }
            password = requiredPassword(password, "New password");
            if (!password.equals(request.confirmPassword())) throw badRequest("Password and confirmation do not match");
        }
        if (!email.equals(String.valueOf(account.get("email"))) && email.equals(adminEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already assigned to the Super Admin");
        }
        ensureUsernameAvailable(email, administratorId);
        try {
            if (password.isBlank()) {
                jdbc.update("UPDATE admin_console.administrators SET email = ? WHERE id = ?", email, administratorId);
            } else {
                jdbc.update("UPDATE admin_console.administrators SET email = ?, password_hash = ? WHERE id = ?",
                        email, passwordEncoder.encode(password), administratorId);
            }
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already in use");
        }
        jdbc.update("UPDATE admin_console.admin_sessions SET email = ? WHERE id = ?", email, sessionId);
        jdbc.update("DELETE FROM admin_console.admin_sessions WHERE administrator_id = ? AND id <> ?", administratorId, sessionId);
        recordEvent(authentication.getName(), "UPDATE_ADMIN_CREDENTIALS", "ADMINISTRATOR", administratorId.toString(),
                Map.of("usernameChanged", !email.equals(String.valueOf(account.get("email"))), "passwordChanged", !password.isBlank()));
        return credentialChangeResponse(administratorId, email, sessionId);
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, Authentication authentication) {
        JwtService.AdminClaims claims = jwt.parseAdmin(bearerToken(request));
        jdbc.update("DELETE FROM admin_console.admin_sessions WHERE id = ?", claims.sessionId());
        recordEvent(authentication.getName(), "LOGOUT", "ADMINISTRATOR", claims.sessionId().toString(), Map.of());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public Map<String, Object> health(Authentication authentication) {
        jdbc.queryForObject("SELECT 1", Integer.class);
        return Map.of("status", "ready", "database", "connected", "administrator", authentication.getName());
    }

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard(@RequestParam(defaultValue = "30") int days) {
        if (days != 7 && days != 30 && days != 90) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "days must be 7, 30, or 90");
        }
        Map<String, Object> users = jdbc.queryForMap("""
                SELECT COUNT(*)::int AS total,
                       COUNT(*) FILTER (WHERE status = 'ACTIVE')::int AS active,
                       COUNT(*) FILTER (WHERE status = 'BLOCKED')::int AS blocked
                FROM public.customers
                """);
        Map<String, Object> transactions = jdbc.queryForMap("""
                SELECT COUNT(*)::int AS total, COALESCE(SUM(amount), 0)::numeric(18,2) AS volume,
                       COUNT(*) FILTER (WHERE status = 'COMPLETED')::int AS successful,
                       COUNT(*) FILTER (WHERE status = 'FAILED')::int AS failed,
                       COUNT(*) FILTER (WHERE status = 'REVERSED')::int AS reversed
                FROM public.transactions
                WHERE created_at >= now() - (? * interval '1 day') AND action_type <> 'BILL_PAYMENT'
                """, days);
        Map<String, Object> bills = jdbc.queryForMap("""
                SELECT COUNT(*)::int AS total, COALESCE(SUM(amount), 0)::numeric(18,2) AS volume,
                       COUNT(*) FILTER (WHERE status = 'FAILED')::int AS failed
                FROM public.transactions
                WHERE created_at >= now() - (? * interval '1 day') AND action_type = 'BILL_PAYMENT'
                """, days);
        Map<String, Object> alerts = jdbc.queryForMap("""
                SELECT COUNT(*)::int AS total,
                       COUNT(*) FILTER (WHERE COALESCE(sc.status, 'OPEN') IN ('OPEN', 'INVESTIGATING'))::int AS open,
                       COUNT(*) FILTER (WHERE a.decision = 'DENY')::int AS critical
                FROM public.audit_log a
                LEFT JOIN admin_console.security_cases sc ON sc.alert_id = a.id
                WHERE a.decision IN ('DENY', 'STEP_UP') AND a.ts >= now() - (? * interval '1 day')
                """, days);
        Map<String, Object> aiActivity = jdbc.queryForMap("""
                SELECT COUNT(*)::int AS total FROM public.audit_log
                WHERE stage IN ('INPUT','INTENT','TOOL_CALL','POLICY','OTP','CONFIRMATION','EXECUTION','RESPONSE','FALLBACK')
                  AND ts >= now() - (? * interval '1 day')
                """, days);
        List<Map<String, Object>> daily = jdbc.queryForList("""
                SELECT date_trunc('day', created_at)::date AS day, COALESCE(SUM(amount), 0)::numeric(18,2) AS amount
                FROM public.transactions
                WHERE created_at >= now() - (? * interval '1 day') AND action_type <> 'BILL_PAYMENT'
                GROUP BY 1 ORDER BY 1
                """, days);
        Map<LocalDate, Object> amountsByDay = new LinkedHashMap<>();
        daily.forEach(row -> amountsByDay.put(((java.sql.Date) row.get("day")).toLocalDate(), row.get("amount")));
        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate firstDay = LocalDate.now().minusDays(days - 1L);
        for (int i = 0; i < days; i++) {
            LocalDate date = firstDay.plusDays(i);
            trend.add(Map.of("label", date.toString(), "value", amountsByDay.getOrDefault(date, 0)));
        }
        List<Map<String, Object>> statusCounts = jdbc.queryForList("""
                SELECT status, COUNT(*)::int AS count FROM public.transactions
                WHERE created_at >= now() - (? * interval '1 day') AND action_type <> 'BILL_PAYMENT'
                GROUP BY status
                """, days);
        Map<String, Object> byStatus = new LinkedHashMap<>(Map.of("successful", 0, "pending", 0, "failed", 0, "reversed", 0));
        for (Map<String, Object> row : statusCounts) {
            String key = switch (String.valueOf(row.get("status"))) {
                case "COMPLETED" -> "successful";
                case "FAILED" -> "failed";
                case "REVERSED" -> "reversed";
                default -> null;
            };
            if (key != null) byStatus.put(key, row.get("count"));
        }
        List<Map<String, Object>> recent = jdbc.queryForList("""
                SELECT id::text AS id, full_name AS "fullName", status, created_at AS "registeredAt"
                FROM public.customers ORDER BY created_at DESC LIMIT 5
                """);
        Map<String, Object> transactionSummary = new LinkedHashMap<>();
        transactionSummary.put("total", transactions.get("total"));
        transactionSummary.put("volume", transactions.get("volume"));
        transactionSummary.put("currency", "LKR");
        transactionSummary.put("successful", transactions.get("successful"));
        transactionSummary.put("pending", 0);
        transactionSummary.put("failed", transactions.get("failed"));
        transactionSummary.put("reversed", transactions.get("reversed"));
        transactionSummary.put("byStatus", byStatus);
        return Map.of(
                "users", Map.of("total", users.get("total"), "active", users.get("active"), "inactive", 0, "suspended", 0, "blocked", users.get("blocked")),
                "transactions", transactionSummary,
                "billPayments", Map.of("total", bills.get("total"), "volume", bills.get("volume"), "currency", "LKR", "failed", bills.get("failed"), "pending", 0),
                "securityAlerts", alerts,
                "aiActivity", aiActivity,
                "transactionTrend", trend,
                "recentRegistrations", recent);
    }

    @GetMapping("/{collection}")
    public Map<String, Object> list(@PathVariable String collection,
                                    @RequestParam Map<String, String> query) {
        String key = collectionKey(collection);
        return listRecords(key, query, MAX_PAGE_SIZE);
    }

    @GetMapping("/security/alerts")
    public Map<String, Object> securityAlerts(@RequestParam Map<String, String> query) {
        return listRecords("security", query, MAX_PAGE_SIZE);
    }

    @GetMapping("/ai/interactions")
    public Map<String, Object> aiInteractions(@RequestParam Map<String, String> query) {
        return listRecords("ai", query, MAX_PAGE_SIZE);
    }

    @GetMapping("/{collection}/{id}")
    public Map<String, Object> detail(@PathVariable String collection, @PathVariable String id) {
        return detailRecord(collectionKey(collection), id);
    }

    @GetMapping("/security/alerts/{id}")
    public Map<String, Object> securityAlertDetail(@PathVariable String id) {
        return detailRecord("security", id);
    }

    @GetMapping("/ai/interactions/{id}")
    public Map<String, Object> aiInteractionDetail(@PathVariable String id) {
        return detailRecord("ai", id);
    }

    @GetMapping("/{collection}/export")
    public ResponseEntity<byte[]> export(@PathVariable String collection,
                                         @RequestParam Map<String, String> query,
                                         Authentication authentication) {
        if (!"csv".equalsIgnoreCase(query.getOrDefault("format", ""))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "format=csv is required");
        }
        String key = collectionKey(collection);
        Map<String, Object> result = listRecords(key, query, 5000);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        recordEvent(authentication.getName(), "EXPORT", collection, null, Map.of("rows", items.size(), "format", "csv"));
        return csvResponse(items, key + "-" + LocalDate.now() + ".csv");
    }

    @GetMapping("/security/alerts/export")
    public ResponseEntity<byte[]> exportSecurityAlerts(@RequestParam Map<String, String> query,
                                                        Authentication authentication) {
        return exportItems("security", query, authentication);
    }

    @GetMapping("/ai/interactions/export")
    public ResponseEntity<byte[]> exportAiInteractions(@RequestParam Map<String, String> query,
                                                        Authentication authentication) {
        return exportItems("ai", query, authentication);
    }

    private ResponseEntity<byte[]> exportItems(String key, Map<String, String> query, Authentication authentication) {
        if (!"csv".equalsIgnoreCase(query.getOrDefault("format", ""))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "format=csv is required");
        }
        Map<String, String> exportQuery = new LinkedHashMap<>(query);
        exportQuery.put("size", "5000");
        Map<String, Object> result = listRecords(key, exportQuery, 5000);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        recordEvent(authentication.getName(), "EXPORT", key, null, Map.of("rows", items.size(), "format", "csv"));
        return csvResponse(items, key + "-" + LocalDate.now() + ".csv");
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> report(@RequestParam String type, @RequestParam String from, @RequestParam String to,
                                         @RequestParam(defaultValue = "csv") String format,
                                         Authentication authentication) {
        if (!"csv".equalsIgnoreCase(format)) {
            throw new ApiException(HttpStatus.NOT_IMPLEMENTED, "NOT_SUPPORTED", "Only CSV report export is supported");
        }
        String collection = switch (type) {
            case "transactions" -> "transactions";
            case "bill-payments" -> "bill-payments";
            case "users" -> "users";
            case "security-alerts" -> "security/alerts";
            case "admin-audit" -> "audit-logs";
            case "ai-activity" -> "ai/interactions";
            default -> throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Unsupported report type");
        };
        Map<String, String> filters = Map.of("from", from, "to", to, "size", "5000");
        String key = collectionKey(collection);
        Map<String, Object> result = listRecords(key, filters, 5000);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        recordEvent(authentication.getName(), "EXPORT", type, null, Map.of("rows", items.size(), "format", "csv"));
        return csvResponse(items, "bank-" + type + "-" + from + "-to-" + to + ".csv");
    }

    @PostMapping("/users")
    @Transactional
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody CreateUserRequest request,
                                                           Authentication authentication) {
        String cif = requiredText(request.cif(), "CIF", 20);
        String fullName = requiredText(request.fullName(), "Full name", 160);
        String mobile = requiredText(request.mobile(), "Mobile number", 20);
        String reason = request.reason() == null || request.reason().isBlank()
                ? "Admin console registration"
                : requiredText(request.reason(), "Registration reason", 500);
        String language = request.preferredLanguage() == null ? "EN" : request.preferredLanguage().trim().toUpperCase();
        if (!cif.matches("[A-Za-z0-9-]+")) throw badRequest("CIF may contain only letters, numbers, and hyphens");
        if (!mobile.matches("[+0-9 ()-]{7,20}")) throw badRequest("Enter a valid mobile number");
        if (!List.of("EN", "SI", "TA").contains(language)) throw badRequest("Preferred language must be EN, SI, or TA");
        String email = request.email() == null || request.email().isBlank() ? null : request.email().trim().toLowerCase();
        if (email != null && (email.length() > 254 || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))) {
            throw badRequest("Enter a valid email address");
        }
        UUID id;
        try {
            id = jdbc.queryForObject("""
                    INSERT INTO public.customers (cif, full_name, mobile, email, preferred_language)
                    VALUES (?, ?, ?, ?, ?) RETURNING id
                    """, UUID.class, cif, fullName, mobile, email, language);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That CIF is already registered");
        }
        recordEvent(authentication.getName(), "CREATE_CUSTOMER", "CUSTOMER", id.toString(), Map.of("reason", reason));
        return ResponseEntity.status(HttpStatus.CREATED).body(jdbc.queryForMap("""
                SELECT id::text AS "customerId", id::text AS id, cif, full_name AS "fullName",
                       CASE WHEN mobile IS NULL THEN NULL
                            ELSE repeat('•', LEAST(8, GREATEST(length(regexp_replace(mobile, '\\D', '', 'g')) - 4, 0)))
                                 || right(regexp_replace(mobile, '\\D', '', 'g'), 4) END AS "mobileMasked",
                       CASE WHEN email IS NULL THEN NULL ELSE left(email, 1) || '•••@' || split_part(email, '@', 2) END AS "emailMasked",
                       status, created_at AS "registeredAt"
                FROM public.customers WHERE id = ?
                """, id));
    }

    @PostMapping("/administrators")
    @Transactional
    public ResponseEntity<Map<String, Object>> createAdministrator(@RequestBody CreateAdminRequest request,
                                                                     Authentication authentication) {
        requireSuperAdmin(authentication);
        String fullName = requiredText(request.fullName(), "Full name", 160);
        String email = requiredText(request.email(), "Username (email)", 254).toLowerCase();
        String password = request.password() == null ? "" : request.password();
        if (!email.matches(ADMIN_EMAIL_PATTERN)) throw badRequest("Enter a valid email address for the username");
        if (email.equals(adminEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already assigned to the Super Admin");
        }
        if (password.length() < 14 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest("Initial password must be at least 14 characters and no more than 72 UTF-8 bytes");
        }

        UUID id;
        try {
            id = jdbc.queryForObject("""
                    INSERT INTO admin_console.administrators
                        (email, full_name, password_hash, role, must_change_credentials, status, created_by)
                    VALUES (?, ?, ?, 'ADMIN', true, 'ACTIVE', ?)
                    RETURNING id
                    """, UUID.class, email, fullName, passwordEncoder.encode(password), authentication.getName());
        } catch (org.springframework.dao.DuplicateKeyException e) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "An administrator with that username already exists");
        }
        recordEvent(authentication.getName(), "CREATE_ADMIN", "ADMINISTRATOR", id.toString(),
                Map.of("email", email, "role", "ADMIN"));
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "administratorId", id.toString(),
                "fullName", fullName,
                "email", email,
                "role", "ADMIN",
                "status", "ACTIVE",
                "mustChangeCredentials", true));
    }

    @PatchMapping("/users/{id}/status")
    @Transactional
    public Map<String, Object> updateUserStatus(@PathVariable UUID id, @RequestBody StatusRequest request,
                                                 Authentication authentication) {
        String status = request.status() == null ? "" : request.status().trim().toUpperCase();
        String reason = requiredText(request.reason(), "Status change reason", 500);
        if (!List.of("ACTIVE", "BLOCKED").contains(status)) {
            throw badRequest("Customer status must be ACTIVE or BLOCKED, as supported by the banking database");
        }
        List<Map<String, Object>> updated = jdbc.queryForList("""
                UPDATE public.customers SET status = ? WHERE id = ? RETURNING id::text AS id, status
                """, status, id);
        if (updated.isEmpty()) throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Customer not found");
        recordEvent(authentication.getName(), "UPDATE_CUSTOMER_STATUS", "CUSTOMER", id.toString(), Map.of("status", status, "reason", reason));
        return updated.getFirst();
    }

    @PatchMapping("/security/alerts/{id}/investigation")
    @Transactional
    public Map<String, Object> updateSecurityCase(@PathVariable long id, @RequestBody InvestigationRequest request,
                                                   Authentication authentication) {
        String status = request.status() == null ? "" : request.status().trim().toUpperCase();
        String notes = requiredText(request.notes(), "Investigation notes", 2000);
        String assignedTo = request.assignedTo() == null || request.assignedTo().isBlank()
                ? null : request.assignedTo().trim();
        if (!List.of("OPEN", "INVESTIGATING", "RESOLVED", "FALSE_POSITIVE").contains(status)) {
            throw badRequest("Invalid investigation status");
        }
        if (assignedTo != null && assignedTo.length() > 120) throw badRequest("Assignee must be no longer than 120 characters");
        Integer exists = jdbc.queryForObject("""
                SELECT COUNT(*)::int FROM public.audit_log WHERE id = ? AND decision IN ('DENY', 'STEP_UP')
                """, Integer.class, id);
        if (exists == null || exists == 0) throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Security signal not found");
        jdbc.update("""
                INSERT INTO admin_console.security_cases (alert_id, status, notes, assigned_to, updated_by)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (alert_id) DO UPDATE SET status = EXCLUDED.status, notes = EXCLUDED.notes,
                    assigned_to = EXCLUDED.assigned_to, updated_by = EXCLUDED.updated_by, updated_at = now()
                """, id, status, notes, assignedTo, authentication.getName());
        recordEvent(authentication.getName(), "UPDATE_SECURITY_CASE", "SECURITY_SIGNAL", Long.toString(id),
                Map.of("status", status, "assignedTo", assignedTo == null ? "" : assignedTo));
        return Map.of("alertId", id, "status", status, "notes", notes, "assignedTo", assignedTo == null ? "" : assignedTo,
                "updatedBy", authentication.getName());
    }

    private Map<String, Object> listRecords(String key, Map<String, String> query, int requestedMaxSize) {
        List<Object> params = new ArrayList<>();
        List<String> where = new ArrayList<>();
        String source;
        String select;
        String timestamp;
        String statusField = null;
        List<String> statusOptions;
        switch (key) {
            case "users" -> {
                source = "public.customers";
                select = """
                        id::text AS id, id::text AS "customerId", cif, full_name AS "fullName",
                        CASE WHEN mobile IS NULL OR length(regexp_replace(mobile, '\\D', '', 'g')) <= 4 THEN '••••'
                             ELSE repeat('•', LEAST(8, length(regexp_replace(mobile, '\\D', '', 'g')) - 4))
                                  || right(regexp_replace(mobile, '\\D', '', 'g'), 4) END AS "mobileMasked",
                        CASE WHEN email IS NULL THEN NULL ELSE left(email, 1) || '•••@' || split_part(email, '@', 2) END AS "emailMasked",
                        status, created_at AS "registeredAt"
                        """;
                timestamp = "created_at";
                statusField = "status";
                statusOptions = List.of("ACTIVE", "BLOCKED");
                addSearch(query, params, where, "full_name", "cif", "mobile", "COALESCE(email, '')");
            }
            case "transactions", "bills" -> {
                source = "public.transactions t JOIN public.customers c ON c.id = t.customer_id";
                select = """
                        t.id::text AS id, t.id::text AS "transactionId", t.txn_ref AS "transactionRef",
                        t.action_type AS "transactionType", c.full_name AS "customerName", t.amount,
                        t.currency, CASE t.status WHEN 'COMPLETED' THEN 'SUCCESSFUL' ELSE t.status END AS status,
                        t.created_at AS "createdAt"
                        """;
                timestamp = "t.created_at";
                statusField = "t.status";
                statusOptions = List.of("SUCCESSFUL", "FAILED", "REVERSED");
                where.add(key.equals("bills") ? "t.action_type = 'BILL_PAYMENT'" : "t.action_type <> 'BILL_PAYMENT'");
                addSearch(query, params, where, "t.txn_ref", "t.action_type", "c.full_name", "t.counterparty::text");
                if (key.equals("transactions") && query.containsKey("transactionType") && !query.get("transactionType").isBlank()) {
                    where.add("t.action_type = ?");
                    params.add(query.get("transactionType").trim());
                }
            }
            case "security" -> {
                source = """
                        public.audit_log a LEFT JOIN public.customers c ON c.id = a.customer_id
                        LEFT JOIN admin_console.security_cases sc ON sc.alert_id = a.id
                        """;
                select = """
                        a.id::text AS id, a.id::text AS "alertId", a.stage, a.decision,
                        COALESCE(sc.status, 'OPEN') AS status,
                        COALESCE(sc.notes, a.explanation, array_to_string(a.reason_codes, ', '), 'Policy decision requires review') AS reason,
                        CASE WHEN a.decision = 'DENY' THEN 'HIGH' ELSE 'MEDIUM' END AS severity,
                        c.full_name AS "customerName", a.ts AS "createdAt", sc.assigned_to AS "assignedTo"
                        """;
                timestamp = "a.ts";
                statusField = "COALESCE(sc.status, 'OPEN')";
                statusOptions = List.of("OPEN", "INVESTIGATING", "RESOLVED", "FALSE_POSITIVE");
                where.add("a.decision IN ('DENY', 'STEP_UP')");
                addSearch(query, params, where, "a.explanation", "array_to_string(a.reason_codes, ', ')", "c.full_name", "a.decision", "a.stage");
                if (query.containsKey("severity") && !query.get("severity").isBlank()) {
                    where.add("(CASE WHEN a.decision = 'DENY' THEN 'HIGH' ELSE 'MEDIUM' END) = ?");
                    params.add(query.get("severity").trim().toUpperCase());
                }
            }
            case "ai" -> {
                source = "public.audit_log a LEFT JOIN public.customers c ON c.id = a.customer_id";
                select = """
                        a.id::text AS id, a.id::text AS "interactionId",
                        CASE WHEN c.cif IS NULL THEN NULL ELSE '••' || right(c.cif, 2) END AS "customerReference",
                        a.intent, CASE WHEN a.tool_name IS NULL THEN ARRAY[]::text[] ELSE ARRAY[a.tool_name] END AS tools,
                        COALESCE(a.outcome, a.decision, a.stage) AS status,
                        a.latency_ms AS "latencyMs", a.ts AS "createdAt"
                        """;
                timestamp = "a.ts";
                statusField = "COALESCE(a.outcome, a.decision, a.stage)";
                statusOptions = List.of("ALLOW", "STEP_UP", "DENY", "COMPLETED", "FAILED", "FALLBACK", "INPUT", "RESPONSE");
                where.add("a.stage IN ('INPUT','INTENT','TOOL_CALL','POLICY','OTP','CONFIRMATION','EXECUTION','RESPONSE','FALLBACK')");
                addSearch(query, params, where, "a.intent", "a.tool_name", "a.stage", "a.decision", "a.outcome");
            }
            case "audit" -> {
                source = "admin_console.admin_events";
                select = """
                        id::text AS id, id::text AS "eventId", admin_email AS "administratorName",
                        action, resource_type AS "resourceType", outcome, created_at AS "createdAt",
                        resource_id AS "resourceId", details
                        """;
                timestamp = "created_at";
                statusField = "outcome";
                statusOptions = List.of("SUCCESS", "DENIED");
                addSearch(query, params, where, "admin_email", "action", "resource_type", "resource_id", "outcome");
                if (query.containsKey("action") && !query.get("action").isBlank()) {
                    where.add("action ILIKE ?");
                    params.add("%" + escapeLike(query.get("action").trim()) + "%");
                }
                if (query.containsKey("outcome") && !query.get("outcome").isBlank()) {
                    where.add("outcome = ?");
                    params.add(query.get("outcome").trim().toUpperCase());
                }
            }
            case "administrators" -> {
                String term = query.getOrDefault("q", "").trim().toLowerCase();
                String roleFilter = query.getOrDefault("role", "").trim();
                String statusFilter = query.getOrDefault("status", "").trim();
                List<Map<String, Object>> rows = new ArrayList<>();
                rows.add(administrator("bootstrap-admin"));
                rows.addAll(jdbc.queryForList("""
                        SELECT id::text AS "administratorId", full_name AS "fullName", email,
                               role, status, must_change_credentials AS "mustChangeCredentials",
                               created_at AS "createdAt", last_login_at AS "lastLoginAt"
                        FROM admin_console.administrators
                        ORDER BY created_at DESC
                        """));
                rows.removeIf(row ->
                        (!term.isBlank() && !String.valueOf(row.get("email")).toLowerCase().contains(term)
                                && !String.valueOf(row.get("fullName")).toLowerCase().contains(term))
                                || (!roleFilter.isBlank() && !roleFilter.equalsIgnoreCase(String.valueOf(row.get("role"))))
                                || (!statusFilter.isBlank() && !statusFilter.equalsIgnoreCase(String.valueOf(row.get("status")))));
                int adminPage = pageValue(query.get("page"));
                int adminSize = Math.max(1, Math.min(pageValue(query.get("size"), 20), requestedMaxSize));
                return paged(rows.subList(Math.min(adminPage * adminSize, rows.size()),
                                Math.min((adminPage + 1) * adminSize, rows.size())),
                        rows.size(), query, List.of("ACTIVE"), Map.of("role", List.of("SUPER_ADMIN", "ADMIN")),
                        adminPage, adminSize);
            }
            default -> throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Admin collection not found");
        }
        addDateFilters(query, params, where, timestamp);
        if (statusField != null && query.containsKey("status") && !query.get("status").isBlank()) {
            String status = query.get("status").trim().toUpperCase();
            if ((key.equals("transactions") || key.equals("bills")) && status.equals("SUCCESSFUL")) status = "COMPLETED";
            where.add(statusField + " = ?");
            params.add(status);
        }
        String whereSql = where.isEmpty() ? "" : " WHERE " + String.join(" AND ", where);
        int page = pageValue(query.get("page"));
        int size = Math.min(pageValue(query.get("size"), requestedMaxSize == 5000 ? 5000 : 20), requestedMaxSize);
        if (size < 1) size = 20;
        Integer total = jdbc.queryForObject("SELECT COUNT(*) FROM " + source + whereSql, Integer.class, params.toArray());
        List<Object> dataParams = new ArrayList<>(params);
        dataParams.add(size);
        dataParams.add((long) page * size);
        List<Map<String, Object>> rows = jdbc.queryForList("SELECT " + select + " FROM " + source + whereSql
                + " ORDER BY " + timestamp + " DESC LIMIT ? OFFSET ?", dataParams.toArray());
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("statuses", statusOptions);
        if (key.equals("transactions")) {
            filters.put("transactionType", jdbc.queryForList("SELECT DISTINCT action_type FROM public.transactions ORDER BY action_type")
                    .stream().map(row -> row.get("action_type")).toList());
        }
        if (key.equals("security")) filters.put("severity", List.of("HIGH", "MEDIUM"));
        if (key.equals("audit")) filters.put("action", List.of("LOGIN", "LOGOUT", "CREATE_CUSTOMER", "UPDATE_CUSTOMER_STATUS", "UPDATE_SECURITY_CASE", "EXPORT"));
        if (key.equals("administrators")) filters.put("role", List.of("SUPER_ADMIN", "ADMIN"));
        return paged(rows, total == null ? 0 : total, query, statusOptions, filters, page, size);
    }

    private Map<String, Object> detailRecord(String key, String id) {
        try {
            return switch (key) {
                case "users" -> jdbc.queryForMap("""
                        SELECT id::text AS id, cif, full_name AS "fullName",
                               CASE WHEN mobile IS NULL OR length(regexp_replace(mobile, '\\D', '', 'g')) <= 4 THEN '••••'
                                    ELSE repeat('•', LEAST(8, length(regexp_replace(mobile, '\\D', '', 'g')) - 4))
                                         || right(regexp_replace(mobile, '\\D', '', 'g'), 4) END AS "mobileMasked",
                               CASE WHEN email IS NULL THEN NULL ELSE left(email, 1) || '•••@' || split_part(email, '@', 2) END AS "emailMasked",
                               preferred_language AS "preferredLanguage",
                               status, created_at AS "createdAt"
                        FROM public.customers WHERE id = ?
                        """, UUID.fromString(id));
                case "transactions", "bills" -> jdbc.queryForMap("""
                        SELECT t.id::text AS id, t.txn_ref AS "transactionRef", t.action_type AS "transactionType",
                               c.full_name AS "customerName", t.amount, t.currency, t.status, t.remarks,
                               t.channel, t.created_at AS "createdAt"
                        FROM public.transactions t JOIN public.customers c ON c.id = t.customer_id
                        WHERE t.id = ? AND %s
                        """.formatted(key.equals("bills") ? "t.action_type = 'BILL_PAYMENT'" : "t.action_type <> 'BILL_PAYMENT'"),
                        UUID.fromString(id));
                case "security" -> jdbc.queryForMap("""
                        SELECT a.id::text AS id, a.stage, a.decision, a.reason_codes AS "reasonCodes",
                               COALESCE(sc.status, 'OPEN') AS status, COALESCE(sc.notes, a.explanation) AS notes,
                               sc.assigned_to AS "assignedTo", a.ts AS "createdAt"
                        FROM public.audit_log a LEFT JOIN admin_console.security_cases sc ON sc.alert_id = a.id
                        WHERE a.id = ? AND a.decision IN ('DENY', 'STEP_UP')
                        """, Long.parseLong(id));
                case "ai" -> jdbc.queryForMap("""
                        SELECT id::text AS id, stage, intent, tool_name AS tool, decision, outcome,
                               latency_ms AS "latencyMs", redacted_utterance AS "redactedUtterance", ts AS "createdAt"
                        FROM public.audit_log
                        WHERE id = ? AND stage IN ('INPUT','INTENT','TOOL_CALL','POLICY','OTP','CONFIRMATION','EXECUTION','RESPONSE','FALLBACK')
                        """, Long.parseLong(id));
                case "audit" -> jdbc.queryForMap("""
                        SELECT id::text AS id, admin_email AS "administratorName", action,
                               resource_type AS "resourceType", resource_id AS "resourceId",
                               outcome, details, created_at AS "createdAt"
                        FROM admin_console.admin_events WHERE id = ?
                        """, UUID.fromString(id));
                case "administrators" -> {
                    if ("bootstrap-admin".equals(id)) yield administrator(id);
                    yield jdbc.queryForMap("""
                            SELECT id::text AS "administratorId", full_name AS "fullName", email,
                                   role, status, must_change_credentials AS "mustChangeCredentials",
                                   created_at AS "createdAt", last_login_at AS "lastLoginAt"
                            FROM admin_console.administrators WHERE id = ?
                            """, UUID.fromString(id));
                }
                default -> throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Admin collection not found");
            };
        } catch (org.springframework.dao.EmptyResultDataAccessException e) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Record not found");
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid record identifier");
        }
    }

    private Map<String, Object> administrator(String id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("administratorId", id);
        result.put("fullName", adminName);
        result.put("email", adminEmail);
        result.put("role", "SUPER_ADMIN");
        result.put("status", "ACTIVE");
        result.put("lastLoginAt", jdbc.query("SELECT max(created_at) FROM admin_console.admin_events WHERE action = 'LOGIN' AND admin_email = ?",
                rs -> rs.next() ? rs.getObject(1, OffsetDateTime.class) : null, adminEmail));
        result.put("permissions", PERMISSIONS);
        result.put("capabilities", CAPABILITIES);
        return result;
    }

    private Map<String, Object> administratorProfile(Map<String, Object> account) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", String.valueOf(account.get("id")));
        result.put("administratorId", String.valueOf(account.get("id")));
        result.put("fullName", account.get("full_name"));
        result.put("email", account.get("email"));
        result.put("role", account.get("role"));
        result.put("status", account.get("status"));
        result.put("mustChangeCredentials", Boolean.TRUE.equals(account.get("must_change_credentials")));
        result.put("lastLoginAt", account.get("last_login_at"));
        result.put("permissions", PERMISSIONS);
        result.put("capabilities", CAPABILITIES);
        return result;
    }

    private Map<String, Object> credentialChangeResponse(UUID administratorId, String email, UUID sessionId) {
        Map<String, Object> account = jdbc.queryForMap("""
                SELECT id, email, full_name, role, status, must_change_credentials, last_login_at
                FROM admin_console.administrators WHERE id = ?
                """, administratorId);
        return Map.of("accessToken", jwt.issueAdmin(email, sessionId),
                "administrator", administratorProfile(account), "expiresIn", jwt.ttlSeconds());
    }

    private String requiredEmail(String value, String fieldName) {
        String email = requiredText(value, fieldName, 254).toLowerCase();
        if (!email.matches(ADMIN_EMAIL_PATTERN)) throw badRequest(fieldName + " must be a valid email address");
        return email;
    }

    private static String requiredPassword(String value, String fieldName) {
        if (value == null || value.length() < 14
                || value.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest(fieldName + " must be at least 14 characters and no more than 72 UTF-8 bytes");
        }
        return value;
    }

    private void ensureUsernameAvailable(String email, UUID exceptAdministratorId) {
        if (email.equals(adminEmail)) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already assigned to the Super Admin");
        }
        Integer matches = jdbc.queryForObject("""
                SELECT COUNT(*)::int FROM admin_console.administrators
                WHERE lower(email) = ? AND id <> ?
                """, Integer.class, email, exceptAdministratorId);
        if (matches != null && matches > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "EXISTS", "That username is already in use");
        }
    }

    private void recordEvent(String email, String action, String type, String id, Map<String, Object> details) {
        jdbc.update("""
                INSERT INTO admin_console.admin_events (id, admin_email, action, resource_type, resource_id, outcome, details)
                VALUES (?, ?, ?, ?, ?, 'SUCCESS', ?::jsonb)
                """, UUID.randomUUID(), email, action, type, id, toJson(details));
    }

    private static String toJson(Map<String, Object> map) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(map);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize admin audit details", e);
        }
    }

    private boolean configuredAdmin() {
        return !adminEmail.isBlank() && adminEmail.matches(ADMIN_EMAIL_PATTERN)
                && adminPassword.length() >= 14;
    }

    private void requireSuperAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !adminEmail.equalsIgnoreCase(authentication.getName())
                || authentication.getAuthorities().stream().noneMatch(authority -> "ROLE_SUPER_ADMIN".equals(authority.getAuthority()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Only the Super Admin can register administrators");
        }
    }

    private static String collectionKey(String collection) {
        return switch (collection) {
            case "users" -> "users";
            case "transactions" -> "transactions";
            case "bill-payments" -> "bills";
            case "security/alerts" -> "security";
            case "ai/interactions" -> "ai";
            case "audit-logs" -> "audit";
            case "administrators" -> "administrators";
            default -> throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Admin collection not found");
        };
    }

    private static ResponseEntity<byte[]> csvResponse(List<Map<String, Object>> items, String filename) {
        List<String> lines = new ArrayList<>();
        if (!items.isEmpty()) lines.add(String.join(",", items.getFirst().keySet().stream().map(AdminController::csvCell).toList()));
        for (Map<String, Object> row : items) lines.add(String.join(",", row.values().stream().map(AdminController::csvCell).toList()));
        byte[] csv = String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename.replace("\"", "") + "\"")
                .body(csv);
    }

    private static String csvCell(Object value) {
        String text = value == null ? "" : value instanceof Map<?, ?> || value instanceof List<?>
                ? toJsonValue(value) : String.valueOf(value);
        if (text.matches("^[=+\\-@\\t\\r].*")) text = "'" + text;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    private static String toJsonValue(Object value) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(value);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize CSV value", e);
        }
    }

    private static Map<String, Object> paged(List<Map<String, Object>> items, int total, Map<String, String> query,
                                              List<String> statuses, Map<String, Object> filters, int page, int size) {
        Map<String, Object> safeFilters = new LinkedHashMap<>(filters);
        safeFilters.putIfAbsent("statuses", statuses);
        return Map.of("items", items, "page", page, "size", Math.max(1, size), "totalElements", total,
                "totalPages", total == 0 ? 0 : (total + Math.max(1, size) - 1) / Math.max(1, size), "filters", safeFilters);
    }

    private static void addSearch(Map<String, String> query, List<Object> params, List<String> where, String... columns) {
        String term = query.getOrDefault("q", "").trim();
        if (term.isEmpty()) return;
        String predicate = String.join(" OR ", java.util.Arrays.stream(columns).map(column -> column + " ILIKE ?").toList());
        where.add("(" + predicate + ")");
        String pattern = "%" + escapeLike(term.substring(0, Math.min(term.length(), 120))) + "%";
        for (int i = 0; i < columns.length; i++) params.add(pattern);
    }

    private static void addDateFilters(Map<String, String> query, List<Object> params, List<String> where, String column) {
        LocalDate from = parseDate(query.get("from"), "from");
        LocalDate to = parseDate(query.get("to"), "to");
        if (from != null && to != null && from.isAfter(to)) throw badRequest("The from date must not be after the to date");
        if (from != null) {
            where.add(column + " >= ?");
            params.add(from);
        }
        if (to != null) {
            where.add(column + " < (?::date + interval '1 day')");
            params.add(to);
        }
    }

    private static LocalDate parseDate(String value, String name) {
        if (value == null || value.isBlank()) return null;
        try {
            return LocalDate.parse(value);
        } catch (java.time.DateTimeException e) {
            throw badRequest(name + " must be a valid YYYY-MM-DD date");
        }
    }

    private static String requiredText(String value, String name, int maxLength) {
        String text = value == null ? "" : value.trim();
        if (text.isEmpty() || text.length() > maxLength) throw badRequest(name + " is required and must not exceed " + maxLength + " characters");
        return text;
    }

    private static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private static int pageValue(String value) {
        return pageValue(value, 0);
    }

    private static int pageValue(String value, int fallback) {
        try {
            return Math.max(0, Math.min(Integer.parseInt(value), 1_000_000));
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String bearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Not authenticated");
        return header.substring(7).trim();
    }
}
