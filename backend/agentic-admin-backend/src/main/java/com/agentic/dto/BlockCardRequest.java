package com.agentic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO to block a card.
 */
public class BlockCardRequest {
    
    @NotNull(message = "Card ID is required")
    private Long cardId;

    @NotBlank(message = "Reason is required")
    private String reason;               // "Lost card", "Stolen card", "Suspicious activity"

    public BlockCardRequest() {
    }

    public BlockCardRequest(Long cardId, String reason) {
        this.cardId = cardId;
        this.reason = reason;
    }

    // ===== GETTERS AND SETTERS =====

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
