package com.agentic.dto;

import com.agentic.entity.Beneficiary;

import java.time.LocalDateTime;

/**
 * Safe Beneficiary DTO for API responses
 */
public class BeneficiaryResponse {
    
    private Long id;
    private String nickname;
    private String accountHolderName;
    private String accountNumber;
    private String status;
    private LocalDateTime createdAt;
    
    public BeneficiaryResponse() {
    }
    
    public BeneficiaryResponse(Beneficiary beneficiary) {
        this.id = beneficiary.getId();
        this.nickname = beneficiary.getNickname();
        this.accountHolderName = beneficiary.getAccountHolderName();
        this.accountNumber = beneficiary.getAccount().getAccountNumber();
        this.status = beneficiary.getStatus().toString();
        this.createdAt = beneficiary.getCreatedAt();
    }
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public String getAccountNumber() {
        return accountNumber;
    }
    
    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
