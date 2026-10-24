package com.lunorlabs.bank.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service to dispatch real-time events to connected customers and bank administrators.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final BankingWebSocketHandler handler;
    private final ObjectMapper objectMapper;

    public NotificationService(BankingWebSocketHandler handler, ObjectMapper objectMapper) {
        this.handler = handler;
        this.objectMapper = objectMapper;
    }

    /**
     * Send an event to all active sessions of a specific customer.
     */
    public void notifyCustomer(UUID customerId, String eventType, Object payload) {
        if (customerId == null) return;
        Set<WebSocketSession> sessions = handler.getCustomerSessions(customerId);
        if (sessions.isEmpty()) {
            log.debug("No active WebSocket sessions for customer {}", customerId);
            return;
        }

        String json = createEventJson(eventType, payload);
        log.info("Dispatching event '{}' to customer {} across {} active session(s)", eventType, customerId, sessions.size());
        sendToSessions(sessions, json);
    }

    /**
     * Send an event to all connected bank administrators.
     */
    public void notifyAdmins(String eventType, Object payload) {
        Set<WebSocketSession> admins = handler.getAdminSessions();
        if (admins.isEmpty()) {
            log.debug("No active admin WebSocket sessions");
            return;
        }

        String json = createEventJson(eventType, payload);
        log.info("Dispatching event '{}' to {} admin session(s)", eventType, admins.size());
        sendToSessions(admins, json);
    }

    /**
     * Broadcast an event to every connected client (all customers + all admins).
     */
    public void broadcast(String eventType, Object payload) {
        String json = createEventJson(eventType, payload);
        sendToSessions(handler.getAdminSessions(), json);
        for (Set<WebSocketSession> sessions : handler.getAllCustomerSessions().values()) {
            sendToSessions(sessions, json);
        }
    }

    private String createEventJson(String eventType, Object payload) {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("event", eventType);
        event.put("timestamp", Instant.now().toString());
        event.put("data", payload != null ? payload : Map.of());
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize WebSocket event {}: {}", eventType, e.getMessage());
            return "{\"event\":\"" + eventType + "\",\"timestamp\":\"" + Instant.now() + "\"}";
        }
    }

    private void sendToSessions(Set<WebSocketSession> sessions, String json) {
        TextMessage message = new TextMessage(json);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.sendMessage(message);
                    }
                } catch (IOException e) {
                    log.warn("Error sending message to WebSocket session {}: {}", session.getId(), e.getMessage());
                }
            }
        }
    }
}
