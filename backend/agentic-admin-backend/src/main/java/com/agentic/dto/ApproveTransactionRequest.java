package com.agentic.dto;

import jakarta.validation.constraints.NotBlank;

public class ApproveTransactionRequest {
    
    @NotBlank(message = "Notes are required")
    private String notes;
    
    // ===== GETTERS AND SETTERS =====
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
}
