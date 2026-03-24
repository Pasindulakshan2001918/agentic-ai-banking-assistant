package com.agentic.dto.ai;

/**
 * AI Service Balance Query Request DTO
 * Simplified contract for balance retrieval
 */
public class AiBalanceRequest {
    private Long userId;
    private Long accountId;
    private String accountType;  // Optional: "SAVINGS", "CURRENT", "CREDIT", etc.

    public AiBalanceRequest() {}

    public AiBalanceRequest(Long userId, Long accountId) {
        this.userId = userId;
        this.accountId = accountId;
    }

    // Getters & Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }
}
