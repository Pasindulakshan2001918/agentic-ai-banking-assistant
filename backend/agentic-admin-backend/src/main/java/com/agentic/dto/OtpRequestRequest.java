package com.agentic.dto;

public class OtpRequestRequest {
    
    private Long fromAccountId;
    private Long toAccountId;
    private java.math.BigDecimal amount;
    
    public OtpRequestRequest() {
    }
    
    public OtpRequestRequest(Long fromAccountId, Long toAccountId, java.math.BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
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
    
    public java.math.BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(java.math.BigDecimal amount) {
        this.amount = amount;
    }
}
