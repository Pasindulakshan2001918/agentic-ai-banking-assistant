package com.agentic.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

/**
 * Request DTO for instant transfer by beneficiary nickname
 * Replaces Map<String, Object> with strongly-typed, validated fields
 */
public class InstantTransferBeneficiaryRequest {
    
    @NotNull(message = "Source account ID is required")
    private Long fromAccountId;
    
    @NotBlank(message = "Beneficiary nickname is required")
    private String beneficiaryNickname;
    
    @NotNull(message = "Transfer amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0.00")
    @Digits(integer = 15, fraction = 2, message = "Amount must be a valid currency value")
    private BigDecimal amount;
    
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    public InstantTransferBeneficiaryRequest() {
    }

    public InstantTransferBeneficiaryRequest(Long fromAccountId, String beneficiaryNickname, BigDecimal amount) {
        this.fromAccountId = fromAccountId;
        this.beneficiaryNickname = beneficiaryNickname;
        this.amount = amount;
    }

    public Long getFromAccountId() {
        return fromAccountId;
    }

    public void setFromAccountId(Long fromAccountId) {
        this.fromAccountId = fromAccountId;
    }

    public String getBeneficiaryNickname() {
        return beneficiaryNickname;
    }

    public void setBeneficiaryNickname(String beneficiaryNickname) {
        this.beneficiaryNickname = beneficiaryNickname;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
