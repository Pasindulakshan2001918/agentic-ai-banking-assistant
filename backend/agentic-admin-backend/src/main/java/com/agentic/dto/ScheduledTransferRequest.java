package com.agentic.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * CREATE SCHEDULED TRANSFER REQUEST DTO
 */
public class ScheduledTransferRequest {
    
    @NotNull(message = "From account ID required")
    private Long fromAccountId;
    
    @NotNull(message = "To account ID required")
    private Long toAccountId;
    
    @NotNull(message = "Amount required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    
    private String description;
    
    @NotNull(message = "Execution date/time required")
    private LocalDateTime executeAt;
    
    public ScheduledTransferRequest() {}
    
    public ScheduledTransferRequest(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                   String description, LocalDateTime executeAt) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.description = description;
        this.executeAt = executeAt;
    }
    
    // Getters & Setters
    
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getExecuteAt() {
        return executeAt;
    }
    
    public void setExecuteAt(LocalDateTime executeAt) {
        this.executeAt = executeAt;
    }
}
