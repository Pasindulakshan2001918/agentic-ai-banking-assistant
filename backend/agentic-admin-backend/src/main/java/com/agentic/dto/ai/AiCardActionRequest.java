package com.agentic.dto.ai;

/**
 * AI Service Card Action Request DTO
 * Simplified contract for card block/unblock operations
 */
public class AiCardActionRequest {
    private Long userId;
    private Long cardId;
    private String action;  // "BLOCK", "UNBLOCK"
    private String reason;  // "LOST", "STOLEN", "FRAUD", etc.

    public AiCardActionRequest() {}

    public AiCardActionRequest(Long userId, Long cardId, String action) {
        this.userId = userId;
        this.cardId = cardId;
        this.action = action;
    }

    // Getters & Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
