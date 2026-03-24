package com.agentic.dto.ai;

import java.math.BigDecimal;

/**
 * AI Service Payment Request DTO
 * Simplified contract for bill/payment operations
 */
public class AiPaymentRequest {
    private Long userId;
    private Long billId;
    private BigDecimal amount;
    private String amountFormatted;  // "5k", "1 lakh", "10000"
    private String billType;  // "ELECTRICITY", "WATER", "INTERNET", etc.
    private String provider;
    private String referenceNumber;

    public AiPaymentRequest() {}

    public AiPaymentRequest(Long userId, Long billId, BigDecimal amount, String billType) {
        this.userId = userId;
        this.billId = billId;
        this.amount = amount;
        this.billType = billType;
    }

    // Getters & Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getAmountFormatted() {
        return amountFormatted;
    }

    public void setAmountFormatted(String amountFormatted) {
        this.amountFormatted = amountFormatted;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }
}
