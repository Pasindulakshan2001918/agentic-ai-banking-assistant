package com.agentic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO to unblock or toggle card settings — requires OTP.
 */
public class CardActionRequest {
    
    @NotNull(message = "Card ID is required")
    private Long cardId;

    @NotBlank(message = "OTP is required")
    private String otp;                  // required for re-enable and toggle actions

    public CardActionRequest() {
    }

    public CardActionRequest(Long cardId, String otp) {
        this.cardId = cardId;
        this.otp = otp;
    }

    // ===== GETTERS AND SETTERS =====

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }
}
