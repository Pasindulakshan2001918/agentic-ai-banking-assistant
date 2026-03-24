package com.agentic.dto.ai;

import java.time.LocalDateTime;

/**
 * AI Service Card Action Response DTO
 * AI-optimized response for card operations
 */
public class AiCardActionResponse {
    private String operationId;
    private String status;  // "SUCCESS", "FAILED"
    private String message;
    private Long cardId;
    private String cardLast4;
    private String currentStatus;  // "ACTIVE", "BLOCKED", "INACTIVE"
    private String action;  // "BLOCKED", "UNBLOCKED"
    private LocalDateTime actionTime;

    public AiCardActionResponse() {}

    public AiCardActionResponse(String operationId, String status, String message, Long cardId, String action) {
        this.operationId = operationId;
        this.status = status;
        this.message = message;
        this.cardId = cardId;
        this.action = action;
        this.actionTime = LocalDateTime.now();
    }

    // Getters & Setters
    public String getOperationId() {
        return operationId;
    }

    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    public void setCardLast4(String cardLast4) {
        this.cardLast4 = cardLast4;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }

    public void setCurrentStatus(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public LocalDateTime getActionTime() {
        return actionTime;
    }

    public void setActionTime(LocalDateTime actionTime) {
        this.actionTime = actionTime;
    }
}
