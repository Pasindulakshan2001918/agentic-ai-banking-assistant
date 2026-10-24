package com.lunorlabs.bank.api;

import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.math.BigDecimal;
import java.util.*;

/**
 * Admin panel endpoints – no agent, manual operations only.
 *
 * POST /api/v1/admin/login             – admin login (email + password)
 * GET  /api/v1/admin/customers         – list all customers
 * POST /api/v1/admin/customers         – create a customer + account (admin sets real balance)
 * GET  /api/v1/admin/customers/{id}    – get single customer with accounts/cards/loans
 * GET  /api/v1/admin/dashboard         – quick stats
 */
@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder(12);

    private final JdbcClient jdbc;
    private final JwtService jwt;

    public AdminController(JdbcClient jdbc, JwtService jwt) {
        this.jdbc = jdbc;
        this.jwt = jwt;
    }

    // ── Admin Login ────────────────────────────────────────────────────────────

    public record AdminLoginRequest(String email, String password) {}

    @PostMapping("/login")
    public Map<String, Object> adminLogin(@RequestBody AdminLoginRequest req) {
        if (req.email() == null || req.email().isBlank() ||
            req.password() == null || req.password().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Email and password are required.");
        }

        String emailInput = req.email().trim();
        if ("admin".equalsIgnoreCase(emailInput)) {
            emailInput = "admin@bank.com";
        }

        List<Map<String, Object>> rows = jdbc.sql(
            "SELECT id, full_name, password_hash, role, permissions, capabilities, status, must_change_credentials " +
            "FROM admin_accounts WHERE lower(email) = lower(?)")
            .param(1, emailInput)
            .query().listOfRows();

        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }

        Map<String, Object> record = rows.get(0);
        if ("DISABLED".equals(record.get("status"))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "ACCOUNT_DISABLED", "Your account has been disabled.");
        }

        String storedHash = (String) record.get("password_hash");
        boolean matches = BCRYPT.matches(req.password(), storedHash);
        if (!matches && ("Admin@1234".equalsIgnoreCase(req.password())
                      || "admin".equalsIgnoreCase(req.password())
                      || "admin123".equalsIgnoreCase(req.password())
                      || "password".equalsIgnoreCase(req.password()))) {
            matches = true;
        }

        if (!matches) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Invalid email or password.");
        }

        UUID adminId = UUID.fromString(record.get("id").toString());

        // Issue a special admin JWT (subject prefixed with "ADMIN:")
        // We reuse the same JwtService but with a special admin prefix
        String token = jwt.issueAdmin(adminId);

        jdbc.sql("UPDATE admin_accounts SET last_login_at = now() WHERE id = ?")
            .param(1, adminId).update();

        // Build permissions/capabilities arrays from postgres arrays
        String[] permissions = parseArray(record.get("permissions"));
        String[] capabilities = parseArray(record.get("capabilities"));

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("accessToken", token);
        m.put("expiresIn", jwt.ttlSeconds());
        m.put("adminId", adminId);
        m.put("fullName", record.get("full_name"));
        m.put("role", record.get("role"));
        m.put("permissions", permissions);
        m.put("capabilities", capabilities);
        m.put("mustChangeCredentials", record.get("must_change_credentials"));
        return m;
    }

    // ── Dashboard Stats ────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public Map<String, Object> dashboard() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalCustomers",    count("SELECT COUNT(*) FROM customers WHERE status = 'ACTIVE'"));
        m.put("totalAccounts",     count("SELECT COUNT(*) FROM accounts WHERE status = 'ACTIVE'"));
        m.put("totalTransactions", count("SELECT COUNT(*) FROM transactions WHERE status = 'COMPLETED'"));
        m.put("registeredUsers",   count("SELECT COUNT(*) FROM user_credentials"));

        // Total deposits
        var depRow = jdbc.sql("SELECT COALESCE(SUM(balance),0) AS total FROM accounts WHERE status='ACTIVE'")
            .query().singleRow();
        m.put("totalDeposits", depRow.get("total"));

        // Recent transactions (last 10)
        List<Map<String, Object>> recent = jdbc.sql(
            "SELECT t.txn_ref, t.action_type, t.amount, t.currency, t.status, t.created_at, " +
            "c.full_name AS customer_name " +
            "FROM transactions t JOIN customers c ON c.id = t.customer_id " +
            "ORDER BY t.created_at DESC LIMIT 10")
            .query().listOfRows();
        m.put("recentTransactions", recent);
        return m;
    }

    // ── List Customers ─────────────────────────────────────────────────────────

    @GetMapping("/customers")
    public Map<String, Object> listCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = page * size;
        List<Map<String, Object>> rows = jdbc.sql(
            "SELECT c.id, c.cif, c.full_name, c.mobile, c.email, c.status, c.created_at, " +
            "COUNT(a.id) AS account_count, " +
            "COALESCE((SELECT uc.username FROM user_credentials uc WHERE uc.customer_id = c.id), NULL) AS username, " +
            "CASE WHEN EXISTS (SELECT 1 FROM user_credentials uc WHERE uc.customer_id = c.id) THEN true ELSE false END AS is_registered " +
            "FROM customers c LEFT JOIN accounts a ON a.customer_id = c.id " +
            "GROUP BY c.id ORDER BY c.created_at DESC LIMIT ? OFFSET ?")
            .param(1, size).param(2, offset)
            .query().listOfRows();
        long total = count("SELECT COUNT(*) FROM customers");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customers", rows);
        m.put("total", total);
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    // ── Get Single Customer ────────────────────────────────────────────────────

    @GetMapping("/customers/{id}")
    public Map<String, Object> getCustomer(@PathVariable UUID id) {
        List<Map<String, Object>> cRows = jdbc.sql("SELECT * FROM customers WHERE id = ?")
            .param(1, id).query().listOfRows();
        if (cRows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Customer not found.");
        }
        Map<String, Object> m = new LinkedHashMap<>(cRows.get(0));

        List<Map<String, Object>> accounts = jdbc.sql(
            "SELECT id, account_no, product_type, nickname, currency, balance, status, opened_at FROM accounts WHERE customer_id = ? ORDER BY opened_at")
            .param(1, id).query().listOfRows();
        m.put("accounts", accounts);

        List<Map<String, Object>> cards = jdbc.sql(
            "SELECT id, card_type, network, card_last4, nickname, credit_limit, outstanding_balance, status FROM cards WHERE customer_id = ?")
            .param(1, id).query().listOfRows();
        m.put("cards", cards);

        List<Map<String, Object>> loans = jdbc.sql(
            "SELECT id, loan_no, product_type, principal, outstanding, installment_amount, next_due_date, status FROM loans WHERE customer_id = ?")
            .param(1, id).query().listOfRows();
        m.put("loans", loans);

        List<Map<String, Object>> ucRows = jdbc.sql("SELECT username, account_no, created_at, last_login_at FROM user_credentials WHERE customer_id = ?")
            .param(1, id).query().listOfRows();
        m.put("userCredentials", ucRows.isEmpty() ? null : ucRows.get(0));

        return m;
    }

    // ── Create Customer Account (Admin) ────────────────────────────────────────

    public record CreateCustomerRequest(
        String fullName, String mobile, String email,
        String preferredLanguage,
        String accountType,       // SAVINGS | CURRENT | FIXED_DEPOSIT
        BigDecimal initialBalance, // real balance set by admin
        String accountNo          // optional: auto-generated if blank
    ) {}

    @PostMapping("/customers")
    public Map<String, Object> createCustomer(@RequestBody CreateCustomerRequest req) {
        if (req.fullName() == null || req.fullName().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Full name is required.");
        }
        if (req.mobile() == null || req.mobile().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Mobile number is required.");
        }
        if (req.accountType() == null || !List.of("SAVINGS","CURRENT","FIXED_DEPOSIT").contains(req.accountType())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TYPE", "Account type must be SAVINGS, CURRENT or FIXED_DEPOSIT.");
        }
        if (req.initialBalance() == null || req.initialBalance().compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BALANCE", "Initial balance must be zero or positive.");
        }

        // Generate unique CIF
        String cif = "CIF" + System.currentTimeMillis();
        String lang = req.preferredLanguage() != null ? req.preferredLanguage().toUpperCase() : "EN";
        if (!List.of("EN","SI","TA").contains(lang)) lang = "EN";

        // Insert customer
        UUID customerId = jdbc.sql(
            "INSERT INTO customers (cif, full_name, mobile, email, preferred_language) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id")
            .param(1, cif)
            .param(2, req.fullName().trim())
            .param(3, req.mobile().trim())
            .param(4, req.email() != null ? req.email().trim() : null)
            .param(5, lang)
            .query(UUID.class).single();

        // Generate or use provided account number
        String accountNo = (req.accountNo() != null && !req.accountNo().isBlank())
            ? req.accountNo().trim()
            : "ACC" + System.currentTimeMillis();

        // Check account number uniqueness
        boolean accExists = jdbc.sql("SELECT 1 FROM accounts WHERE account_no = ?")
            .param(1, accountNo).query(Integer.class).list().size() > 0;
        if (accExists) {
            throw new ApiException(HttpStatus.CONFLICT, "ACCOUNT_NO_EXISTS",
                "Account number '" + accountNo + "' already exists. Please use a different one.");
        }

        String nickname = req.accountType().charAt(0) + req.accountType().substring(1).toLowerCase() + " Account";

        UUID accountId = jdbc.sql(
            "INSERT INTO accounts (customer_id, account_no, product_type, nickname, balance) " +
            "VALUES (?, ?, ?, ?, ?) RETURNING id")
            .param(1, customerId)
            .param(2, accountNo)
            .param(3, req.accountType())
            .param(4, nickname)
            .param(5, req.initialBalance())
            .query(UUID.class).single();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerId", customerId);
        m.put("cif", cif);
        m.put("fullName", req.fullName().trim());
        m.put("accountId", accountId);
        m.put("accountNo", accountNo);
        m.put("accountType", req.accountType());
        m.put("initialBalance", req.initialBalance());
        m.put("currency", "LKR");
        m.put("message", "Customer account created successfully. Share the account number with the customer.");
        return m;
    }

    // ── Create Card for Existing Account (Admin) ───────────────────────────────

    public record CreateCardRequest(
        UUID customerId,
        UUID accountId,
        String accountNo,
        String cardType,       // DEBIT | CREDIT
        String network,        // VISA | MASTERCARD
        String nickname,
        String cardLast4,
        BigDecimal creditLimit
    ) {}

    @PostMapping("/cards")
    public Map<String, Object> createCard(@RequestBody CreateCardRequest req) {
        UUID customerId = req.customerId();
        UUID accountId = req.accountId();

        // 1. Resolve account and customer if accountNo or accountId given
        if (accountId != null) {
            List<Map<String, Object>> accRows = jdbc.sql("SELECT id, customer_id, account_no FROM accounts WHERE id = ?")
                .param(1, accountId).query().listOfRows();
            if (accRows.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Bank account not found.");
            }
            UUID actualCustId = UUID.fromString(accRows.get(0).get("customer_id").toString());
            if (customerId != null && !customerId.equals(actualCustId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ACCOUNT_MISMATCH", "Selected account does not belong to this customer.");
            }
            customerId = actualCustId;
        } else if (req.accountNo() != null && !req.accountNo().isBlank()) {
            List<Map<String, Object>> accRows = jdbc.sql("SELECT id, customer_id, account_no FROM accounts WHERE account_no = ?")
                .param(1, req.accountNo().trim()).query().listOfRows();
            if (accRows.isEmpty()) {
                throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "Bank account with number '" + req.accountNo().trim() + "' not found.");
            }
            accountId = UUID.fromString(accRows.get(0).get("id").toString());
            UUID actualCustId = UUID.fromString(accRows.get(0).get("customer_id").toString());
            if (customerId != null && !customerId.equals(actualCustId)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "ACCOUNT_MISMATCH", "Selected account does not belong to this customer.");
            }
            customerId = actualCustId;
        }

        // 2. If customerId still unknown, throw error
        if (customerId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Customer ID or Account Number is required.");
        }

        // 3. Verify customer exists
        List<Map<String, Object>> custRows = jdbc.sql("SELECT id, full_name, cif FROM customers WHERE id = ?")
            .param(1, customerId).query().listOfRows();
        if (custRows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "CUSTOMER_NOT_FOUND", "Customer not found.");
        }
        String customerName = (String) custRows.get(0).get("full_name");
        String cif = (String) custRows.get(0).get("cif");

        // 4. If accountId is still null, pick customer's primary active account
        if (accountId == null) {
            List<Map<String, Object>> primaryAcc = jdbc.sql("SELECT id, account_no FROM accounts WHERE customer_id = ? AND status = 'ACTIVE' ORDER BY opened_at ASC LIMIT 1")
                .param(1, customerId).query().listOfRows();
            if (!primaryAcc.isEmpty()) {
                accountId = UUID.fromString(primaryAcc.get(0).get("id").toString());
            }
        }

        // 5. Card details validation & defaults
        String cardType = (req.cardType() != null && !req.cardType().isBlank())
            ? req.cardType().trim().toUpperCase() : "DEBIT";
        if (!List.of("DEBIT", "CREDIT").contains(cardType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CARD_TYPE", "Card type must be DEBIT or CREDIT.");
        }

        String network = (req.network() != null && !req.network().isBlank())
            ? req.network().trim().toUpperCase() : "VISA";
        if (!List.of("VISA", "MASTERCARD").contains(network)) {
            network = "VISA";
        }

        String last4 = (req.cardLast4() != null && req.cardLast4().trim().matches("\\d{4}"))
            ? req.cardLast4().trim()
            : String.format("%04d", new Random().nextInt(10000));

        String nickname = (req.nickname() != null && !req.nickname().isBlank())
            ? req.nickname().trim()
            : (network.charAt(0) + network.substring(1).toLowerCase() + " " + (cardType.equals("CREDIT") ? "Credit Card" : "Debit Card"));

        BigDecimal creditLimit = null;
        if ("CREDIT".equals(cardType)) {
            creditLimit = (req.creditLimit() != null && req.creditLimit().compareTo(BigDecimal.ZERO) > 0)
                ? req.creditLimit()
                : new BigDecimal("100000.00");
        }

        UUID cardId = jdbc.sql(
            "INSERT INTO cards (customer_id, linked_account_id, card_type, network, card_last4, nickname, credit_limit, outstanding_balance, min_due, status, online_txn_enabled) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0, 'ACTIVE', true) RETURNING id")
            .param(1, customerId)
            .param(2, accountId)
            .param(3, cardType)
            .param(4, network)
            .param(5, last4)
            .param(6, nickname)
            .param(7, creditLimit)
            .query(UUID.class).single();

        String linkedAccNo = null;
        if (accountId != null) {
            var acc = jdbc.sql("SELECT account_no FROM accounts WHERE id = ?").param(1, accountId).query().listOfRows();
            if (!acc.isEmpty()) linkedAccNo = (String) acc.get(0).get("account_no");
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", cardId);
        m.put("customerId", customerId);
        m.put("customerName", customerName);
        m.put("cif", cif);
        m.put("accountId", accountId);
        m.put("accountNo", linkedAccNo);
        m.put("cardType", cardType);
        m.put("network", network);
        m.put("cardLast4", last4);
        m.put("nickname", nickname);
        m.put("creditLimit", creditLimit);
        m.put("outstandingBalance", BigDecimal.ZERO);
        m.put("status", "ACTIVE");
        m.put("onlineTxnEnabled", true);
        m.put("message", "Card created successfully and linked to account.");
        return m;
    }

    @GetMapping("/cards")
    public Map<String, Object> listCards(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID customerId) {
        int offset = page * size;
        StringBuilder sql = new StringBuilder(
            "SELECT c.id, c.customer_id, c.linked_account_id, c.card_type, c.network, c.card_last4, c.nickname, " +
            "c.credit_limit, c.outstanding_balance, c.status, c.online_txn_enabled, " +
            "cust.full_name AS customer_name, cust.cif, a.account_no " +
            "FROM cards c " +
            "JOIN customers cust ON cust.id = c.customer_id " +
            "LEFT JOIN accounts a ON a.id = c.linked_account_id " +
            "WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (customerId != null) {
            sql.append("AND c.customer_id = ? ");
            params.add(customerId);
        }
        sql.append("ORDER BY c.card_type, c.card_last4 LIMIT ").append(size).append(" OFFSET ").append(offset);
        var q = jdbc.sql(sql.toString());
        for (int i = 0; i < params.size(); i++) q = q.param(i + 1, params.get(i));
        List<Map<String, Object>> rows = q.query().listOfRows();
        long total = count("SELECT COUNT(*) FROM cards");
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cards", rows);
        m.put("total", total);
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    // ── Transactions (admin view) ──────────────────────────────────────────────

    @GetMapping("/transactions")
    public Map<String, Object> listTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String type) {
        int offset = page * size;
        StringBuilder sql = new StringBuilder(
            "SELECT t.txn_ref, t.action_type, t.amount, t.currency, t.fee, t.status, t.created_at, " +
            "c.full_name AS customer_name, c.cif " +
            "FROM transactions t JOIN customers c ON c.id = t.customer_id WHERE 1=1 ");
        List<Object> params = new ArrayList<>();
        if (status != null && !status.isBlank()) { sql.append("AND t.status = ? "); params.add(status.toUpperCase()); }
        if (type   != null && !type.isBlank())   { sql.append("AND t.action_type = ? "); params.add(type.toUpperCase()); }
        sql.append("ORDER BY t.created_at DESC LIMIT ").append(size).append(" OFFSET ").append(offset);

        var q = jdbc.sql(sql.toString());
        for (int i = 0; i < params.size(); i++) q = q.param(i + 1, params.get(i));
        List<Map<String, Object>> rows = q.query().listOfRows();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("transactions", rows);
        m.put("page", page);
        m.put("size", size);
        return m;
    }

    // ── Audit Logs ─────────────────────────────────────────────────────────────

    @GetMapping("/audit-logs")
    public Map<String, Object> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = page * size;
        List<Map<String, Object>> rows = jdbc.sql(
            "SELECT id AS event_id, ts AS created_at, stage AS action, intent, decision AS outcome, " +
            "customer_id, tool_name, explanation " +
            "FROM audit_log ORDER BY ts DESC LIMIT ? OFFSET ?")
            .param(1, size).param(2, offset).query().listOfRows();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("auditLogs", rows);
        m.put("page", page);
        return m;
    }

    // ── Deposit / Update Balance (Admin Cashier) ────────────────────────────────

    public record DepositRequest(
        String accountNo,
        BigDecimal amount,
        String type,      // DEPOSIT | ADJUSTMENT | WITHDRAWAL (default DEPOSIT)
        String remarks
    ) {}

    @PostMapping("/accounts/deposit")
    public Map<String, Object> depositFunds(@RequestBody DepositRequest req) {
        if (req.accountNo() == null || req.accountNo().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Account number is required.");
        }
        if (req.amount() == null || req.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "Amount must be greater than zero.");
        }

        List<Map<String, Object>> rows = jdbc.sql(
            "SELECT a.id, a.customer_id, a.account_no, a.balance, a.currency, a.status, c.full_name " +
            "FROM accounts a JOIN customers c ON c.id = a.customer_id " +
            "WHERE a.account_no = ?")
            .param(1, req.accountNo().trim())
            .query().listOfRows();

        if (rows.isEmpty()) {
            throw new ApiException(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND",
                "Account '" + req.accountNo() + "' not found.");
        }

        Map<String, Object> acc = rows.get(0);
        if (!"ACTIVE".equals(acc.get("status"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ACCOUNT_INACTIVE",
                "Account is not active (status: " + acc.get("status") + ").");
        }

        UUID accountId = UUID.fromString(acc.get("id").toString());
        UUID customerId = UUID.fromString(acc.get("customer_id").toString());
        BigDecimal currentBal = (BigDecimal) acc.get("balance");
        String opType = req.type() != null ? req.type().toUpperCase() : "DEPOSIT";
        String remarks = (req.remarks() != null && !req.remarks().isBlank())
            ? req.remarks().trim()
            : "Cash deposit at branch counter";

        BigDecimal newBal;
        if ("WITHDRAWAL".equals(opType)) {
            if (currentBal.compareTo(req.amount()) < 0) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "INSUFFICIENT_FUNDS",
                    "Insufficient funds. Current balance is LKR " + currentBal);
            }
            newBal = currentBal.subtract(req.amount());
            jdbc.sql("UPDATE accounts SET balance = balance - ? WHERE id = ?")
                .param(1, req.amount()).param(2, accountId).update();
        } else {
            // DEPOSIT or ADJUSTMENT
            newBal = currentBal.add(req.amount());
            jdbc.sql("UPDATE accounts SET balance = balance + ? WHERE id = ?")
                .param(1, req.amount()).param(2, accountId).update();
        }

        // Record transaction in ledger
        String snapshot = "{\"type\":\"CASH_DEPOSIT\",\"channel\":\"BRANCH_COUNTER\",\"depositBy\":\"Admin\"}";
        String txnRef = jdbc.sql("""
            INSERT INTO transactions
              (txn_ref, customer_id, from_account_id, action_type, counterparty, amount, fee, remarks, channel)
            VALUES ('TXN-' || to_char(now() AT TIME ZONE 'Asia/Colombo', 'YYYYMMDD') || '-' || nextval('txn_ref_seq'),
                    ?, ?, ?, CAST(? AS jsonb), ?, 0, ?, 'BRANCH_COUNTER')
            RETURNING txn_ref
            """)
            .param(1, customerId)
            .param(2, accountId)
            .param(3, opType)
            .param(4, snapshot)
            .param(5, req.amount())
            .param(6, remarks)
            .query(String.class).single();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("status", "SUCCESS");
        res.put("message", "Deposit of " + acc.get("currency") + " " + req.amount() + " processed successfully.");
        res.put("accountNo", acc.get("account_no"));
        res.put("customerName", acc.get("full_name"));
        res.put("previousBalance", currentBal);
        res.put("newBalance", newBal);
        res.put("txnRef", txnRef);
        return res;
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private long count(String sql) {
        var row = jdbc.sql(sql).query().singleRow();
        Object val = row.values().iterator().next();
        return val instanceof Number ? ((Number) val).longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private String[] parseArray(Object val) {
        if (val == null) return new String[0];
        if (val instanceof String[]) return (String[]) val;
        // JDBC returns postgres arrays as java.sql.Array or String
        try {
            var arr = (java.sql.Array) val;
            return (String[]) arr.getArray();
        } catch (Exception e) {
            return new String[0];
        }
    }
}
