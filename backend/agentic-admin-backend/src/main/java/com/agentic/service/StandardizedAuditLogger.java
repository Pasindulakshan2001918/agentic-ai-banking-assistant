package com.agentic.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

/**
 * AUDIT LOGGER (Standardized)
 * Used by all services to ensure consistent audit trail
 */
@Service
public class StandardizedAuditLogger {
    
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    
    public StandardizedAuditLogger(AuditService auditService) {
        this.auditService = auditService;
        this.objectMapper = new ObjectMapper();
    }
    
    /**
     * Standard CREATE action
     */
    public void logCreate(String entityType, Long entityId, Long userId, Object newValue) {
        auditService.logAction(
            entityType,
            entityId,
            "CREATE",
            "USER_" + userId,
            null,              // ✅ No old value for CREATE
            toJson(newValue),   // ✅ New value captured
            entityType + " created by user " + userId
        );
    }
    
    /**
     * Standard UPDATE action
     */
    public void logUpdate(String entityType, Long entityId, Long userId, Object oldValue, Object newValue) {
        auditService.logAction(
            entityType,
            entityId,
            "UPDATE",
            "USER_" + userId,
            toJson(oldValue),   // ✅ Old value
            toJson(newValue),   // ✅ New value
            entityType + " updated by user " + userId
        );
    }
    
    /**
     * Standard DELETE action
     */
    public void logDelete(String entityType, Long entityId, Long userId, Object oldValue) {
        auditService.logAction(
            entityType,
            entityId,
            "DELETE",
            "USER_" + userId,
            toJson(oldValue),   // ✅ Old value captured before deletion
            null,               // ✅ No new value for DELETE
            entityType + " deleted by user " + userId
        );
    }
    
    /**
     * Standard TRANSFER action
     */
    public void logTransfer(Long transactionId, Long fromAccountId, Long toAccountId, 
                           java.math.BigDecimal amount, String status, Long userId) {
        String description = String.format(
            "Transfer: %s → %s, Amount: %s, Status: %s",
            fromAccountId, toAccountId, amount, status
        );
        
        auditService.logAction(
            "Transaction",
            transactionId,
            "TRANSFER",
            "USER_" + userId,
            null,
            description,
            description
        );
    }
    
    /**
     * Standard OTP action
     */
    public void logOtpGenerated(Long otpId, Long userId, String reference) {
        auditService.logAction(
            "OTP",
            otpId,
            "GENERATE",
            "SYSTEM",
            null,
            "OTP generated for " + reference,
            "OTP generated for reference: " + reference
        );
    }
    
    /**
     * Standard OTP VERIFY action
     */
    public void logOtpVerified(Long otpId, Long userId, String reference, boolean success) {
        String status = success ? "VERIFIED" : "FAILED";
        auditService.logAction(
            "OTP",
            otpId,
            status,
            "USER_" + userId,
            null,
            "OTP " + (success ? "verified" : "verification failed") + " for " + reference,
            "OTP verification attempt for reference: " + reference
        );
    }
    
    /**
     * Helper: Convert object to JSON
     */
    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
