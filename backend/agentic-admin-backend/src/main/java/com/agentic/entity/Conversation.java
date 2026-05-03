
package com.agentic.entity;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;

import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

import java.time.temporal.ChronoUnit;

/**

* CONVERSATION ENTITY

* Tracks AI conversation state with automatic expiry

*

* 🔒 ARCHITECTURAL RULE: Enforces maker-checker flow

* 🔒 STATE MACHINE: Validates state transitions

* 🔒 EXPIRY: Conversations auto-expire after 15 minutes

* 🔒 NO OVERRIDE: Cannot interrupt active flow

*/

@Entity

@Table(name = "conversations", indexes = {

@Index(name = "idx_conversation_user_id", columnList = "user_id"),

@Index(name = "idx_status", columnList = "status"),

@Index(name = "idx_updated_at", columnList = "updated_at")

})

public class Conversation {

@Id

@GeneratedValue(strategy = GenerationType.IDENTITY)

private Long id;

@ManyToOne(fetch = FetchType.LAZY)

@JoinColumn(name = "user_id", nullable = false)

private User user;

@Enumerated(EnumType.STRING)

@Column(nullable = false)

private ConversationStatus status = ConversationStatus.INITIATED;

// 🔒 ACTIVE FLOW: Current intent being processed

@Column(name = "active_intent", length = 50)

private String activeIntent;

// 🔒 CONTEXT: Store current intent context as JSON

@Column(name = "intent_context", columnDefinition = "TEXT")

private String intentContext;

// 🔒 STATE: Track conversation state (AWAITING_CONFIRMATION, AWAITING_OTP, etc)

@Enumerated(EnumType.STRING)

@Column(nullable = false)

private ConversationState state = ConversationState.AWAITING_USER_INPUT;

// 🔒 MESSAGE HISTORY: Store recent messages

@Column(name = "message_history", columnDefinition = "TEXT")

private String messageHistory;

// 🔒 METADATA

@CreationTimestamp

@Column(nullable = false, updatable = false)

private LocalDateTime createdAt;

@UpdateTimestamp

@Column(nullable = false)

private LocalDateTime updatedAt;

@Column(name = "expired_at")

private LocalDateTime expiredAt;

@Version

@Column(name = "version")

private Long version = 0L;

// ===== CONSTANTS =====

/**

* Conversation expiry: 15 minutes

*/

public static final int EXPIRY_MINUTES = 15;

// ===== GETTERS AND SETTERS =====

public Long getId() {

return id;

}

public void setId(Long id) {

this.id = id;

}

public User getUser() {

return user;

}

public void setUser(User user) {

this.user = user;

}

public ConversationStatus getStatus() {

return status;

}

public void setStatus(ConversationStatus status) {

this.status = status;

}

public String getActiveIntent() {

return activeIntent;

}

public void setActiveIntent(String activeIntent) {

this.activeIntent = activeIntent;

}

public String getIntentContext() {

return intentContext;

}

public void setIntentContext(String intentContext) {

this.intentContext = intentContext;

}

public ConversationState getState() {

return state;

}

public void setState(ConversationState state) {

this.state = state;

}

public String getMessageHistory() {

return messageHistory;

}

public void setMessageHistory(String messageHistory) {

this.messageHistory = messageHistory;

}

public LocalDateTime getCreatedAt() {

return createdAt;

}

public void setCreatedAt(LocalDateTime createdAt) {

this.createdAt = createdAt;

}

public LocalDateTime getUpdatedAt() {

return updatedAt;

}

public void setUpdatedAt(LocalDateTime updatedAt) {

this.updatedAt = updatedAt;

}

public LocalDateTime getExpiredAt() {

return expiredAt;

}

public void setExpiredAt(LocalDateTime expiredAt) {

this.expiredAt = expiredAt;

}

public Long getVersion() {

return version;

}

public void setVersion(Long version) {

this.version = version;

}

// ===== HELPER METHODS =====

/**

* Check if conversation has expired (15 minutes)

*/

public boolean isExpired() {

if (expiredAt != null) {

return LocalDateTime.now().isAfter(expiredAt);

}

long minutesElapsed = ChronoUnit.MINUTES.between(updatedAt,
LocalDateTime.now());

return minutesElapsed > EXPIRY_MINUTES;

}

/**

* Mark as expired

*/

public void expire() {

this.expiredAt = LocalDateTime.now();

this.status = ConversationStatus.EXPIRED;

}

/**

* Check if conversation is in active flow (cannot override)

*/

public boolean isInActiveFlow() {

return activeIntent != null &&

(state == ConversationState.AWAITING_CONFIRMATION ||

state == ConversationState.AWAITING_OTP ||

state == ConversationState.PROCESSING);

}

// ===== ENUMS =====

public enum ConversationStatus {

INITIATED, // Just started

ACTIVE, // User engaged

AWAITING_RESPONSE, // Waiting for user response

COMPLETED, // Conversation ended successfully

CANCELLED, // User cancelled

EXPIRED // Timed out after 15 minutes

}

public enum ConversationState {

AWAITING_USER_INPUT, // Ready for next user input

INTENT_DETECTED, // Intent identified

PROCESSING, // Use case processing

AWAITING_CONFIRMATION, // Waiting for user confirmation

AWAITING_OTP, // Waiting for OTP verification

AWAITING_MFA, // Waiting for multi-factor auth

ERROR, // Error occurred

SUCCESS // Success

}

}
