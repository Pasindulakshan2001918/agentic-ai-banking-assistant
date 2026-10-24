package com.lunorlabs.bank.api;

import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.common.Money;
import com.lunorlabs.bank.common.RequestMeta;
import com.lunorlabs.bank.repo.AccountRepo;
import com.lunorlabs.bank.repo.BillerRepo;
import com.lunorlabs.bank.repo.CustomerRepo;
import com.lunorlabs.bank.repo.PayeeRepo;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only lookups the agent needs (contract endpoints 2, 3, 7, 8, 9). All scoped to the JWT's customer. */
@RestController
@RequestMapping("/api/v1")
public class ReadController {
    private final CustomerRepo customers;
    private final AccountRepo accounts;
    private final PayeeRepo payees;
    private final BillerRepo billers;
    private final JdbcClient jdbc;

    public ReadController(CustomerRepo customers, AccountRepo accounts, PayeeRepo payees, BillerRepo billers, JdbcClient jdbc) {
        this.customers = customers;
        this.accounts = accounts;
        this.payees = payees;
        this.billers = billers;
        this.jdbc = jdbc;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest req) {
        RequestMeta meta = RequestMeta.from(req);
        var c = customers.findById(meta.customerId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "NOT_FOUND", "Customer not found"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("customerId", c.id());
        m.put("fullName", c.fullName());
        m.put("preferredLanguage", c.preferredLanguage());
        return m;
    }

    @GetMapping("/accounts")
    public Map<String, Object> accounts(HttpServletRequest req) {
        RequestMeta meta = RequestMeta.from(req);
        List<Map<String, Object>> list = accounts.findByCustomer(meta.customerId()).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.id());
            m.put("accountNo", a.accountNo());
            m.put("masked", "••••" + a.last4());
            m.put("productType", a.productType());
            m.put("nickname", a.nickname());
            m.put("currency", a.currency());
            m.put("balance", a.balance());
            m.put("status", a.status());
            return m;
        }).toList();
        return Map.of("accounts", list);
    }

    @GetMapping("/payees")
    public Map<String, Object> payees(HttpServletRequest req, @RequestParam(required = false) String q) {
        RequestMeta meta = RequestMeta.from(req);
        List<Map<String, Object>> list = payees.search(meta.customerId(), q).stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.id());
            m.put("nickname", p.nickname());
            m.put("beneficiaryName", p.beneficiaryName());
            m.put("bankName", p.bankName());
            m.put("maskedAccount", "••••" + p.last4());
            m.put("addedAt", p.addedAt());
            return m;
        }).toList();
        return Map.of("payees", list);
    }

    @GetMapping("/billers")
    public Map<String, Object> billers(HttpServletRequest req, @RequestParam String q) {
        RequestMeta meta = RequestMeta.from(req);
        List<Map<String, Object>> list = billers.search(q).stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", b.id());
            m.put("code", b.code());
            m.put("name", b.name(meta.lang()));
            m.put("category", b.category());
            m.put("referenceLabel", b.referenceLabel());
            m.put("score", Math.round(b.score() * 100.0) / 100.0);
            return m;
        }).toList();
        return Map.of("billers", list);
    }

    @GetMapping("/billers/registered")
    public Map<String, Object> registered(HttpServletRequest req) {
        RequestMeta meta = RequestMeta.from(req);
        List<Map<String, Object>> list = billers.registered(meta.customerId()).stream().map(rb -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", rb.id());
            m.put("nickname", rb.nickname());
            m.put("billerName", rb.biller().name(meta.lang()));
            m.put("referenceNo", rb.referenceNo());
            m.put("amountDue", rb.amountDue());
            m.put("dueDate", rb.dueDate());
            m.put("amountDueFormatted", rb.amountDue() == null ? null : Money.fmt(rb.amountDue()));
            return m;
        }).toList();
        return Map.of("registeredBillers", list);
    }

    @GetMapping("/cards")
    public Map<String, Object> cards(HttpServletRequest req) {
        RequestMeta meta = RequestMeta.from(req);
        List<Map<String, Object>> list = jdbc.sql("SELECT * FROM cards WHERE customer_id = ? ORDER BY card_type, card_last4")
                .param(1, meta.customerId())
                .query().listOfRows();
        return Map.of("cards", list);
    }

    @GetMapping("/loans")
    public Map<String, Object> loans(HttpServletRequest req) {
        RequestMeta meta = RequestMeta.from(req);
        List<Map<String, Object>> list = jdbc.sql("SELECT * FROM loans WHERE customer_id = ? ORDER BY loan_no")
                .param(1, meta.customerId())
                .query().listOfRows();
        return Map.of("loans", list);
    }

    @PostMapping("/cards/{id}/block")
    public Map<String, Object> blockCard(HttpServletRequest req, @PathVariable java.util.UUID id) {
        RequestMeta meta = RequestMeta.from(req);
        int updated = jdbc.sql("UPDATE cards SET status = 'BLOCKED' WHERE id = ? AND customer_id = ? AND status != 'BLOCKED'")
                .param(1, id).param(2, meta.customerId())
                .update();
        if (updated == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CARD", "Card not found or already blocked.");
        }
        return Map.of("status", "COMPLETED", "message", "Card successfully blocked.");
    }

    @GetMapping("/transactions")
    public Map<String, Object> transactions(HttpServletRequest req,
            @RequestParam(defaultValue = "15") int limit) {
        RequestMeta meta = RequestMeta.from(req);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<Map<String, Object>> list = jdbc.sql(
            "SELECT t.id, t.txn_ref, t.action_type, t.amount, t.fee, t.currency, t.status, " +
            "       t.remarks, t.counterparty, t.channel, t.created_at, a.account_no " +
            "FROM transactions t LEFT JOIN accounts a ON a.id = t.from_account_id " +
            "WHERE t.customer_id = ? ORDER BY t.created_at DESC LIMIT ?")
            .param(1, meta.customerId())
            .param(2, safeLimit)
            .query().listOfRows();
        return Map.of("transactions", list);
    }

    public record AddPayeeRequest(String nickname, String beneficiaryName, String bankCode, String bankName, String accountNo) {}

    @PostMapping("/payees")
    public Map<String, Object> addPayee(HttpServletRequest req, @RequestBody AddPayeeRequest body) {
        RequestMeta meta = RequestMeta.from(req);
        if (body.nickname() == null || body.nickname().isBlank() ||
            body.beneficiaryName() == null || body.beneficiaryName().isBlank() ||
            body.accountNo() == null || body.accountNo().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MISSING_FIELDS", "Nickname, beneficiary name, and account number are required.");
        }
        String bankCode = (body.bankCode() != null && !body.bankCode().isBlank()) ? body.bankCode().trim() : "7010";
        String bankName = (body.bankName() != null && !body.bankName().isBlank()) ? body.bankName().trim() : "Bank of Ceylon";

        java.util.UUID id = jdbc.sql(
            "INSERT INTO payees (customer_id, nickname, beneficiary_name, bank_code, bank_name, account_no) " +
            "VALUES (?, ?, ?, ?, ?, ?) " +
            "ON CONFLICT (customer_id, bank_code, account_no) DO UPDATE SET nickname = EXCLUDED.nickname, beneficiary_name = EXCLUDED.beneficiary_name " +
            "RETURNING id")
            .param(1, meta.customerId())
            .param(2, body.nickname().trim())
            .param(3, body.beneficiaryName().trim())
            .param(4, bankCode)
            .param(5, bankName)
            .param(6, body.accountNo().trim())
            .query(java.util.UUID.class).single();

        return Map.of("id", id, "status", "SUCCESS", "message", "Payee saved successfully.");
    }
}
