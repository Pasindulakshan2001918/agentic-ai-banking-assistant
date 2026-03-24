package com.agentic.dto.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI Service Payment Response DTO
 * AI-optimized response for payment operations
 */
public class AiPaymentResponse {
    private String operationId;
    private String status;  // "SUCCESS", "PENDING", "FAILED"
    private String message;
    private Long paymentId;
    private BigDecimal amountPaid;
    private BigDecimal remainingBalance;
    private String referenceNumber;
    private String billType;
    private LocalDateTime completedAt;

    public AiPaymentResponse() {}

    public AiPaymentResponse(String operationId, String status, String message, Long paymentId, BigDecimal amountPaid) {
        this.operationId = operationId;
        this.status = status;
        this.message = message;
        this.paymentId = paymentId;
        this.amountPaid = amountPaid;
        this.completedAt = LocalDateTime.now();
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

    public Long getPaymentId() {
        return paymentId;
    }

    public void setPaymentId(Long paymentId) {
        this.paymentId = paymentId;
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

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getBillType() {
        return billType;
    }

    public void setBillType(String billType) {
        this.billType = billType;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }
}
