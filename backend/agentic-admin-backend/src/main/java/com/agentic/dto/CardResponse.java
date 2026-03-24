package com.agentic.dto;

/**
 * Response DTO when listing or fetching a card.
 */
public class CardResponse {
    
    private Long cardId;
    private String maskedCardNumber;
    private String lastFourDigits;
    private String cardType;
    private String status;
    private boolean onlinePaymentsEnabled;
    private boolean contactlessEnabled;
    private String expiryDate;           // formatted "MM/YY"
    private String blockReason;          // null if not blocked

    public CardResponse() {
    }

    public CardResponse(Long cardId, String maskedCardNumber, String lastFourDigits,
                       String cardType, String status, boolean onlinePaymentsEnabled,
                       boolean contactlessEnabled, String expiryDate, String blockReason) {
        this.cardId = cardId;
        this.maskedCardNumber = maskedCardNumber;
        this.lastFourDigits = lastFourDigits;
        this.cardType = cardType;
        this.status = status;
        this.onlinePaymentsEnabled = onlinePaymentsEnabled;
        this.contactlessEnabled = contactlessEnabled;
        this.expiryDate = expiryDate;
        this.blockReason = blockReason;
    }

    // ===== GETTERS AND SETTERS =====

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOnlinePaymentsEnabled() {
        return onlinePaymentsEnabled;
    }

    public void setOnlinePaymentsEnabled(boolean onlinePaymentsEnabled) {
        this.onlinePaymentsEnabled = onlinePaymentsEnabled;
    }

    public boolean isContactlessEnabled() {
        return contactlessEnabled;
    }

    public void setContactlessEnabled(boolean contactlessEnabled) {
        this.contactlessEnabled = contactlessEnabled;
    }

    public String getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(String expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }
}
