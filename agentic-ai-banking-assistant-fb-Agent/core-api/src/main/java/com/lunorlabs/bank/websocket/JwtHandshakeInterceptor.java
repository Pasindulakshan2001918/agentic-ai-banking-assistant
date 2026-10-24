package com.lunorlabs.bank.websocket;

import com.lunorlabs.bank.security.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

/**
 * Intercepts the WebSocket handshake to authenticate the client using their JWT token.
 * Token can be passed as ?token=... in the query string or Sec-WebSocket-Protocol.
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private static final Logger log = LoggerFactory.getLogger(JwtHandshakeInterceptor.class);
    private final JwtService jwtService;

    public JwtHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        URI uri = request.getURI();
        String query = uri.getQuery();
        String token = null;

        if (query != null && !query.isBlank()) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "token".equalsIgnoreCase(pair[0])) {
                    token = pair[1].trim();
                    break;
                }
            }
        }

        if (token == null || token.isBlank()) {
            log.warn("WebSocket handshake rejected: missing token query parameter from {}", request.getRemoteAddress());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            UUID userId = jwtService.parse(token);
            boolean isAdmin = jwtService.isAdmin(token);

            attributes.put("userId", userId);
            attributes.put("isAdmin", isAdmin);
            log.info("WebSocket handshake authenticated: userId={}, isAdmin={}", userId, isAdmin);
            return true;
        } catch (Exception e) {
            log.warn("WebSocket handshake rejected: invalid JWT from {}: {}", request.getRemoteAddress(), e.getMessage());
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }
}
