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

/** Demo login by CIF. Exists only when bank.dev-login-enabled=true (dev profile). */
@RestController
@RequestMapping("/api/v1/auth")
@ConditionalOnProperty(name = "bank.dev-login-enabled", havingValue = "true")
public class AuthController {
    public record DevLoginRequest(String cif) {}

    private final CustomerRepo customers;
    private final JwtService jwt;

    public AuthController(CustomerRepo customers, JwtService jwt) {
        this.customers = customers;
        this.jwt = jwt;
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
}
