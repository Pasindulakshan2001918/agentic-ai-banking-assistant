package com.agentic.dto.ai;

import java.math.BigDecimal;

/**
 * AI Service Balance Response DTO
 * AI-optimized response for balance queries
 */
public class AiBalanceResponse {
    private String operationId;
    private String status;  // "SUCCESS", "FAILED"
    private String message;
    private Long accountId;
    private String accountType;
    private BigDecimal availableBalance;
    private BigDecimal totalBalance;
    private BigDecimal creditLimit;
    private BigDecimal usedCredit;
    private String currencyCode;

    public AiBalanceResponse() {}

    public AiBalanceResponse(String operationId, Long accountId, BigDecimal availableBalance, BigDecimal totalBalance) {
        this.operationId = operationId;
        this.status = "SUCCESS";
        this.accountId = accountId;
        this.availableBalance = availableBalance;
        this.totalBalance = totalBalance;
        this.currencyCode = "INR";
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

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getTotalBalance() {
        return totalBalance;
    }

    public void setTotalBalance(BigDecimal totalBalance) {
        this.totalBalance = totalBalance;
    }

    public BigDecimal getCreditLimit() {
        return creditLimit;
    }

    public void setCreditLimit(BigDecimal creditLimit) {
        this.creditLimit = creditLimit;
    }

    public BigDecimal getUsedCredit() {
        return usedCredit;
    }

    public void setUsedCredit(BigDecimal usedCredit) {
        this.usedCredit = usedCredit;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
