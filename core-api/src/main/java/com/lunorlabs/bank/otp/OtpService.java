package com.lunorlabs.bank.otp;

import com.lunorlabs.bank.common.ApiException;
import com.lunorlabs.bank.config.BankProperties;
import com.lunorlabs.bank.repo.CustomerRepo;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpService {
    public enum Kind { VERIFIED, INVALID, EXPIRED, LOCKED, NO_CHALLENGE }

    public record Result(Kind kind, int attemptsLeft) {
        static Result of(Kind k) { return new Result(k, 0); }
    }

    private record Challenge(UUID id, String hash, int attempts, int maxAttempts,
                             OffsetDateTime expiresAt, OffsetDateTime verifiedAt) {}

    private static final SecureRandom RNG = new SecureRandom();

    private final JdbcClient jdbc;
    private final BankProperties props;
    private final OtpSender sender;
    private final CustomerRepo customers;
    private final byte[] pepper;

    public OtpService(JdbcClient jdbc, BankProperties props, OtpSender sender, CustomerRepo customers) {
        this.jdbc = jdbc;
        this.props = props;
        this.sender = sender;
        this.customers = customers;
        String p = props.otp().pepper();
        if (p == null || p.isBlank()) {
            throw new IllegalStateException("bank.otp.pepper (env OTP_PEPPER) must be set");
        }
        this.pepper = p.getBytes(StandardCharsets.UTF_8);
    }

    public boolean hasChallenge(UUID pendingId) {
        return latest(pendingId).isPresent();
    }

    /** Creates and "sends" a new OTP. Returns the code only when dev-echo is on, else null. */
    public String issue(UUID pendingId, UUID customerId) {
        long count = jdbc.sql("SELECT count(*) FROM otp_challenges WHERE pending_action_id = :p")
                .param("p", pendingId).query(Long.class).single();
        if (count >= 1L + props.otp().maxResends()) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "OTP_LIMIT", "Too many OTP requests for this action.");
        }
        String code = String.format("%06d", RNG.nextInt(1_000_000));
        jdbc.sql("""
                INSERT INTO otp_challenges (pending_action_id, customer_id, code_hash, max_attempts, expires_at)
                VALUES (:p, :c, :h, :m, :e)
                """)
                .param("p", pendingId).param("c", customerId).param("h", hash(pendingId, code))
                .param("m", props.otp().maxAttempts())
                .param("e", OffsetDateTime.now().plusMinutes(props.otp().ttlMinutes()))
                .update();
        customers.findById(customerId).ifPresent(c -> sender.send(c.mobile(), code));
        return props.otp().devEcho() ? code : null;
    }

    public Result verify(UUID pendingId, String submitted) {
        Optional<Challenge> opt = latest(pendingId);
        if (opt.isEmpty()) return Result.of(Kind.NO_CHALLENGE);
        Challenge c = opt.get();

        if (c.verifiedAt() != null) return Result.of(Kind.VERIFIED);   // already verified, e.g. retry after a failed execute
        if (OffsetDateTime.now().isAfter(c.expiresAt())) return Result.of(Kind.EXPIRED);
        if (c.attempts() >= c.maxAttempts()) return Result.of(Kind.LOCKED);

        boolean ok = MessageDigest.isEqual(
                hash(pendingId, submitted).getBytes(StandardCharsets.UTF_8),
                c.hash().getBytes(StandardCharsets.UTF_8));
        if (ok) {
            jdbc.sql("UPDATE otp_challenges SET verified_at = now() WHERE id = :id").param("id", c.id()).update();
            return Result.of(Kind.VERIFIED);
        }
        int attempts = jdbc.sql("UPDATE otp_challenges SET attempts = attempts + 1 WHERE id = :id RETURNING attempts")
                .param("id", c.id()).query(Integer.class).single();
        if (attempts >= c.maxAttempts()) return Result.of(Kind.LOCKED);
        return new Result(Kind.INVALID, c.maxAttempts() - attempts);
    }

    private Optional<Challenge> latest(UUID pendingId) {
        return jdbc.sql("""
                SELECT id, code_hash, attempts, max_attempts, expires_at, verified_at
                FROM otp_challenges WHERE pending_action_id = :p
                ORDER BY created_at DESC LIMIT 1
                """)
                .param("p", pendingId)
                .query((rs, i) -> new Challenge(rs.getObject("id", UUID.class), rs.getString("code_hash"),
                        rs.getInt("attempts"), rs.getInt("max_attempts"),
                        rs.getObject("expires_at", OffsetDateTime.class),
                        rs.getObject("verified_at", OffsetDateTime.class)))
                .optional();
    }

    /** HMAC-SHA256(pepper, pendingId:code). Codes are never stored in plain text. */
    private String hash(UUID pendingId, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((pendingId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HMAC unavailable", e);
        }
    }
}
