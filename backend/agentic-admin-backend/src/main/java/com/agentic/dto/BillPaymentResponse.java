package com.agentic.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Response DTO after successful bill payment.
 */
public class BillPaymentResponse {
    
    private String referenceNumber;
    private String providerName;
    private BigDecimal amountPaid;
    private BigDecimal remainingBalance;  // bank account balance after payment
    private LocalDateTime paidAt;
    private String message;

    public BillPaymentResponse() {
    }

    public BillPaymentResponse(String referenceNumber, String providerName,
                              BigDecimal amountPaid, BigDecimal remainingBalance,
                              LocalDateTime paidAt, String message) {
        this.referenceNumber = referenceNumber;
        this.providerName = providerName;
        this.amountPaid = amountPaid;
        this.remainingBalance = remainingBalance;
        this.paidAt = paidAt;
        this.message = message;
    }

    // ===== GETTERS AND SETTERS =====

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public BigDecimal getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(BigDecimal amountPaid) {
        this.amountPaid = amountPaid;
    }

    public BigDecimal getRemainingBalance() {
        return remainingBalance;
    }

    public void setRemainingBalance(BigDecimal remainingBalance) {
        this.remainingBalance = remainingBalance;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
