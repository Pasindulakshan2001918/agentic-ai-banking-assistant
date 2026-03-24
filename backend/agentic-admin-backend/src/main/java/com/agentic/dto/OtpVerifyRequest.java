package com.agentic.dto;

public class OtpVerifyRequest {
    
    private String otpCode;
    private Long fromAccountId;
    private Long toAccountId;
    private java.math.BigDecimal amount;
    
    public OtpVerifyRequest() {
    }
    
    public OtpVerifyRequest(String otpCode, Long fromAccountId, Long toAccountId, java.math.BigDecimal amount) {
        this.otpCode = otpCode;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
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
