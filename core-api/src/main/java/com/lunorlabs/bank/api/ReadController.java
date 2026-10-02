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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

    public ReadController(CustomerRepo customers, AccountRepo accounts, PayeeRepo payees, BillerRepo billers) {
        this.customers = customers;
        this.accounts = accounts;
        this.payees = payees;
        this.billers = billers;
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
}
