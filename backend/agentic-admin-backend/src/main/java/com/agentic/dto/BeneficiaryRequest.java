package com.agentic.dto;

/**
 * Request to add/update beneficiary
 */
public class BeneficiaryRequest {
    
    private String nickname;
    private String accountHolderName;
    private Long accountId;
    
    public BeneficiaryRequest() {
    }
    
    public BeneficiaryRequest(String nickname, String accountHolderName, Long accountId) {
        this.nickname = nickname;
        this.accountHolderName = accountHolderName;
        this.accountId = accountId;
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
    
    public Long getAccountId() {
        return accountId;
    }
    
    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
