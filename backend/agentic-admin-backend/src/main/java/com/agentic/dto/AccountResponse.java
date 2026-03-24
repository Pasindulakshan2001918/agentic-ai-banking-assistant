package com.agentic.dto;

import com.agentic.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Safe Account DTO - Exposes only necessary fields for API responses
 * Prevents entity leaking and schema exposure
 */
public class AccountResponse {
    
    private Long id;
    private String accountNumber;
    private String accountType;
    private BigDecimal balance;
    private String status;
    private String currency;
    private LocalDateTime createdAt;
    
    public AccountResponse() {
    }
    
    public AccountResponse(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.accountType = account.getAccountType().toString();
        this.balance = account.getBalance();
        this.status = account.getStatus().toString();
        this.currency = account.getCurrency();
        this.createdAt = account.getCreatedAt();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getAccountType() {
        return accountType;
    }
    
    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
    
    public BigDecimal getBalance() {
        return balance;
    }
    
    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getCurrency() {
        return currency;
    }
    
    public void setCurrency(String currency) {
        this.currency = currency;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
