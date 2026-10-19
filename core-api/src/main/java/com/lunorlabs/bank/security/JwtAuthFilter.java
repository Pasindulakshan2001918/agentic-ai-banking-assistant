package com.lunorlabs.bank.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Validates customer and administrator JWTs and assigns distinct authorities. */
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final JdbcTemplate jdbc;

    public JwtAuthFilter(JwtService jwt, JdbcTemplate jdbc) {
        this.jwt = jwt;
        this.jdbc = jdbc;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            JwtService.AdminClaims admin = null;
            try {
                admin = jwt.parseAdmin(token);
            } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
                // The token may instead be a customer token.
            }
            if (admin != null) {
                List<Map<String, Object>> sessions = jdbc.queryForList("""
                        SELECT s.role, a.must_change_credentials
                        FROM admin_console.admin_sessions s
                        LEFT JOIN admin_console.administrators a ON a.id = s.administrator_id
                        WHERE s.id = ? AND s.email = ? AND s.expires_at > now()
                          AND ((s.role = 'SUPER_ADMIN' AND s.administrator_id IS NULL)
                               OR (s.role = 'ADMIN' AND a.status = 'ACTIVE'))
                        """, admin.sessionId(), admin.email());
                if (!sessions.isEmpty()) {
                    Map<String, Object> session = sessions.getFirst();
                    String role = String.valueOf(session.get("role"));
                    boolean mustChange = Boolean.TRUE.equals(session.get("must_change_credentials"));
                    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    if (mustChange) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN_CREDENTIALS_ONLY"));
                    } else {
                        authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
                        if ("SUPER_ADMIN".equals(role)) {
                            authorities.add(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));
                        }
                    }
                    SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(admin.email(), null, authorities));
                }
            } else {
                try {
                    UUID id = jwt.parse(token);
                    SecurityContextHolder.getContext()
                            .setAuthentication(new UsernamePasswordAuthenticationToken(id, null, List.of()));
                } catch (io.jsonwebtoken.JwtException | IllegalArgumentException ignored) {
                    // Invalid, expired, or revoked tokens remain anonymous.
                }
            }
        }
        chain.doFilter(req, res);
    }
}
