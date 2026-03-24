package com.agentic.dto;

import com.agentic.entity.ScheduledTransfer;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * SCHEDULED TRANSFER RESPONSE DTO
 */
public class ScheduledTransferResponse {
    
    private Long id;
    private Long fromAccountId;
    private Long toAccountId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private BigDecimal amount;
    private String description;
    private String executeAt;
    private String status;
    private String errorMessage;
    private String createdAt;
    
    public ScheduledTransferResponse() {}
    
    public ScheduledTransferResponse(Long id, Long fromAccountId, Long toAccountId,
                                    String fromAccountNumber, String toAccountNumber,
                                    BigDecimal amount, String description,
                                    LocalDateTime executeAt, String status,
                                    String errorMessage, LocalDateTime createdAt) {
        this.id = id;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.fromAccountNumber = fromAccountNumber;
        this.toAccountNumber = toAccountNumber;
        this.amount = amount;
        this.description = description;
        this.executeAt = executeAt != null ? executeAt.format(DateTimeFormatter.ISO_DATE_TIME) : null;
        this.status = status;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt != null ? createdAt.format(DateTimeFormatter.ISO_DATE_TIME) : null;
    }
    
    /**
     * Convert ScheduledTransfer entity to response DTO
     */
    public static ScheduledTransferResponse fromEntity(ScheduledTransfer st) {
        return new ScheduledTransferResponse(
            st.getId(),
            st.getFromAccount().getId(),
            st.getToAccount().getId(),
            st.getFromAccount().getAccountNumber(),
            st.getToAccount().getAccountNumber(),
            st.getAmount(),
            st.getDescription(),
            st.getExecuteAt(),
            st.getStatus().toString(),
            st.getErrorMessage(),
            st.getCreatedAt()
        );
    }
    
    // Getters & Setters
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
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
    
    public String getFromAccountNumber() {
        return fromAccountNumber;
    }
    
    public void setFromAccountNumber(String fromAccountNumber) {
        this.fromAccountNumber = fromAccountNumber;
    }
    
    public String getToAccountNumber() {
        return toAccountNumber;
    }
    
    public void setToAccountNumber(String toAccountNumber) {
        this.toAccountNumber = toAccountNumber;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getExecuteAt() {
        return executeAt;
    }
    
    public void setExecuteAt(String executeAt) {
        this.executeAt = executeAt;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public String getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
