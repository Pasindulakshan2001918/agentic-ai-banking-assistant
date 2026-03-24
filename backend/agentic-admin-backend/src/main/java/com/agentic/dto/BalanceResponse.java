package com.agentic.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Balance response DTO - Minimal data for balance inquiries
 */
public class BalanceResponse {
    
    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private LocalDateTime timestamp;
    
    public BalanceResponse() {
    }
    
    public BalanceResponse(Long accountId, String accountNumber, BigDecimal balance, 
                          String currency, String status) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.timestamp = LocalDateTime.now();
    }
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}
