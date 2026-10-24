package com.lunorlabs.bank.common;

import com.lunorlabs.bank.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;

import java.util.UUID;

/** Per-request context: who, which chat session, which user turn, which language. */
public record RequestMeta(UUID customerId, UUID sessionId, UUID correlationId, Lang lang) {

    public static RequestMeta from(HttpServletRequest r) {
        return new RequestMeta(
                CurrentUser.id(),
                parse(r.getHeader("X-Session-Id")),
                parse(r.getHeader("X-Correlation-Id")),
                Lang.fromHeader(r.getHeader("Accept-Language")));
    }

    private static UUID parse(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return UUID.fromString(s.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
