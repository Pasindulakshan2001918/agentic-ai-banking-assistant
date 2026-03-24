package com.agentic.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request DTO to pay a bill after OTP verification.
 * Supports custom amount for mobile recharge.
 */
public class BillPaymentRequest {
    
    @NotNull(message = "Bill ID is required")
    private Long billId;

    @NotNull(message = "Account ID is required")
    private Long accountId;       // which bank account to debit

    @NotNull(message = "OTP is required")
    private String otp;

    // for mobile recharge — amount can differ from outstanding
    private BigDecimal customAmount;

    public BillPaymentRequest() {
    }

    public BillPaymentRequest(Long billId, Long accountId, String otp) {
        this.billId = billId;
        this.accountId = accountId;
        this.otp = otp;
    }

    // ===== GETTERS AND SETTERS =====

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public BigDecimal getCustomAmount() {
        return customAmount;
    }

    public void setCustomAmount(BigDecimal customAmount) {
        this.customAmount = customAmount;
    }
}
