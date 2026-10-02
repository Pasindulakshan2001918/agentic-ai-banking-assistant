package com.lunorlabs.bank.security;

import com.lunorlabs.bank.config.BankProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final int ttlMinutes;

    public JwtService(BankProperties props) {
        String secret = props.jwt().secret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("bank.jwt.secret (env JWT_SECRET) must be at least 32 characters");
        }
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMinutes = props.jwt().ttlMinutes();
    }

    public String issue(UUID customerId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(customerId.toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(ttlMinutes * 60L)))
                .signWith(key)
                .compact();
    }

    public int ttlSeconds() { return ttlMinutes * 60; }

    /** @throws io.jsonwebtoken.JwtException if invalid or expired */
    public UUID parse(String token) {
        String sub = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
        return UUID.fromString(sub);
    }
}
