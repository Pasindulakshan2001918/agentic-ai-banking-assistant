package com.agentic.dto;

import jakarta.validation.constraints.NotBlank;

public class RejectTransactionRequest {
    
    @NotBlank(message = "Rejection reason is required")
    private String reason;
    
    // ===== GETTERS AND SETTERS =====
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
}
