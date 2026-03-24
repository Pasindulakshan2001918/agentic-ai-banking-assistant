package com.agentic.dto.ai;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI Service Transfer Response DTO
 * AI-optimized response for transfer operations
 * Includes operation reference, status, and confirmation details
 */
public class AiTransferResponse {
    private String operationId;
    private String status;  // "SUCCESS", "PENDING", "FAILED"
    private String message;
    private Long transactionId;
    private Long fromAccountId;
    private Long toAccountId;
    private BigDecimal amount;
    private String recipient;
    private LocalDateTime timestamp;
    private Boolean requiresConfirmation;
    private String confirmationCode;

    public AiTransferResponse() {}

    public AiTransferResponse(String operationId, String status, String message, Long transactionId, Long fromAccountId, Long toAccountId, BigDecimal amount) {
        this.operationId = operationId;
        this.status = status;
        this.message = message;
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
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

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public Long getToAccountId() {
        return toAccountId;
    }

    public void setToAccountId(Long toAccountId) {
        this.toAccountId = toAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Boolean getRequiresConfirmation() {
        return requiresConfirmation;
    }

    public void setRequiresConfirmation(Boolean requiresConfirmation) {
        this.requiresConfirmation = requiresConfirmation;
    }

    public String getConfirmationCode() {
        return confirmationCode;
    }

    public void setConfirmationCode(String confirmationCode) {
        this.confirmationCode = confirmationCode;
    }
}
