package com.agentic.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for verifying OTP and completing beneficiary transfer
 * Step 2 of OTP-based transfer flow
 */
public class VerifyOtpAndTransferByBeneficiaryRequest {
    
    @NotNull(message = "Transaction ID is required")
    private Long transactionId;
    
    @NotBlank(message = "OTP code is required")
    private String otpCode;

    public VerifyOtpAndTransferByBeneficiaryRequest() {
    }

    public VerifyOtpAndTransferByBeneficiaryRequest(Long transactionId, String otpCode) {
        this.transactionId = transactionId;
        this.otpCode = otpCode;
    }

    public Long getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(Long transactionId) {
        this.transactionId = transactionId;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
