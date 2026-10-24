package com.lunorlabs.bank.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages active WebSocket connections for both bank customers and bank administrators.
 */
@Component
public class BankingWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(BankingWebSocketHandler.class);
    private final ObjectMapper objectMapper;

    // customerId -> Set of sessions (handles multiple devices / tabs)
    private final Map<UUID, Set<WebSocketSession>> customerSessions = new ConcurrentHashMap<>();
    // Set of active admin sessions
    private final Set<WebSocketSession> adminSessions = ConcurrentHashMap.newKeySet();

    public BankingWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        UUID userId = (UUID) session.getAttributes().get("userId");
        Boolean isAdmin = (Boolean) session.getAttributes().get("isAdmin");

        if (Boolean.TRUE.equals(isAdmin)) {
            adminSessions.add(session);
            log.info("Admin WebSocket connected. Total admins: {}", adminSessions.size());
        } else if (userId != null) {
            customerSessions.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
            log.info("Customer {} WebSocket connected. Active customer connections: {}", userId,
                    customerSessions.get(userId).size());
        }

        // Send connection ACK
        try {
            Map<String, Object> ack = Map.of(
                    "event", "CONNECTED",
                    "timestamp", Instant.now().toString(),
                    "data", Map.of("userId", userId != null ? userId.toString() : "", "isAdmin", Boolean.TRUE.equals(isAdmin))
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(ack)));
        } catch (IOException e) {
            log.warn("Failed to send WebSocket ACK to session {}: {}", session.getId(), e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        UUID userId = (UUID) session.getAttributes().get("userId");
        Boolean isAdmin = (Boolean) session.getAttributes().get("isAdmin");

        if (Boolean.TRUE.equals(isAdmin)) {
            adminSessions.remove(session);
            log.info("Admin WebSocket disconnected ({}). Remaining admins: {}", status, adminSessions.size());
        } else if (userId != null) {
            Set<WebSocketSession> sessions = customerSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    customerSessions.remove(userId);
                }
            }
            log.info("Customer {} WebSocket disconnected ({})", userId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.warn("WebSocket transport error for session {}: {}", session.getId(), exception.getMessage());
        try {
            session.close(CloseStatus.SERVER_ERROR);
        } catch (IOException ignored) {}
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload().trim();
        // Support heartbeat ping/pong
        if (payload.contains("PING") || payload.equalsIgnoreCase("ping")) {
            try {
                Map<String, Object> pong = Map.of(
                        "event", "PONG",
                        "timestamp", Instant.now().toString()
                );
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(pong)));
            } catch (IOException e) {
                log.warn("Error sending PONG: {}", e.getMessage());
            }
        }
    }

    public Set<WebSocketSession> getCustomerSessions(UUID customerId) {
        return customerSessions.getOrDefault(customerId, Collections.emptySet());
    }

    public Set<WebSocketSession> getAdminSessions() {
        return adminSessions;
    }

    public Map<UUID, Set<WebSocketSession>> getAllCustomerSessions() {
        return customerSessions;
    }
}
