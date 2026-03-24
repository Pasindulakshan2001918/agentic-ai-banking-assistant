package com.agentic.service;

import com.agentic.entity.AuditLog;
import com.agentic.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AUDIT SERVICE
 * Logs all changes for compliance and debugging
 * 
 * 🔒 PRODUCTION FEATURES:
 * - logAction: Synchronous (for critical operations)
 * - logActionAsync: Asynchronous (doesn't block transaction)
 */
@Service
public class AuditService {
    
    private final AuditLogRepository auditLogRepository;
    private final ApplicationEventPublisher eventPublisher;
    
    public AuditService(AuditLogRepository auditLogRepository, 
                       ApplicationEventPublisher eventPublisher) {
        this.auditLogRepository = auditLogRepository;
        this.eventPublisher = eventPublisher;
    }
    
    /**
     * Log an action synchronously (for critical operations)
     * Used when audit must complete before transaction commits
     */
    @Transactional
    public AuditLog logAction(String entityType, Long entityId, String action, String performedBy,
                             String oldValue, String newValue, String changeDetails, String correlationId) {
        
        AuditLog auditLog = new AuditLog();
        auditLog.setEntityType(entityType);
        auditLog.setEntityId(entityId);
        auditLog.setAction(action);
        auditLog.setPerformedBy(performedBy);
        auditLog.setOldValue(oldValue);
        auditLog.setNewValue(newValue);
        auditLog.setChangeDetails(changeDetails);
        auditLog.setCorrelationId(correlationId);
        
        // Try to capture IP and User-Agent
        try {
            ServletRequestAttributes attributes = 
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIp(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
            }
        } catch (Exception e) {
            // Silent fail if not in HTTP context
        }
        
        return auditLogRepository.save(auditLog);
    }
    
    /**
     * 🔒 Log an action ASYNCHRONOUSLY
     * Non-blocking: doesn't slow down transaction
     * Perfect for logging that shouldn't delay critical operations
     * 
     * Usage: auditService.logActionAsync(...)
     * The log will be written in a separate thread/transaction
     */
    @Async("auditExecutor")
    public void logActionAsync(String entityType, Long entityId, String action, String performedBy,
                              String oldValue, String newValue, String changeDetails, String correlationId) {
        try {
            logAction(entityType, entityId, action, performedBy, oldValue, newValue, changeDetails, correlationId);
        } catch (Exception e) {
            // Log async errors but don't propagate (async method can't throw)
            System.err.println("Async audit logging failed: " + e.getMessage());
        }
    }
    
    /**
     * Backward-compatible: Log without correlationId (will be null)
     */
    @Transactional
    public AuditLog logAction(String entityType, Long entityId, String action, String performedBy,
                             String oldValue, String newValue, String changeDetails) {
        return logAction(entityType, entityId, action, performedBy, oldValue, newValue, changeDetails, null);
    }
    
    /**
     * Backward-compatible: Log async without correlationId (will be null)
     */
    @Async("auditExecutor")
    public void logActionAsync(String entityType, Long entityId, String action, String performedBy,
                              String oldValue, String newValue, String changeDetails) {
        logActionAsync(entityType, entityId, action, performedBy, oldValue, newValue, changeDetails, null);
    }
    
    /**
     * Get audit logs for an entity
     */
    public Page<AuditLog> getAuditLogs(String entityType, Long entityId, Pageable pageable) {
        return auditLogRepository.findByEntityTypeAndEntityId(entityType, entityId, pageable);
    }
    
    /**
     * Get audit logs by action type
     */
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByAction(action, pageable);
    }
    
    /**
     * Get audit logs by entity type
     */
    public Page<AuditLog> getAuditLogsByEntityType(String entityType, Pageable pageable) {
        return auditLogRepository.findByEntityType(entityType, pageable);
    }
    
    /**
     * Get audit logs performed by a user
     */
    public List<AuditLog> getAuditLogsByUser(String username) {
        return auditLogRepository.findByPerformedBy(username);
    }
    
    /**
     * Get audit logs within date range
     */
    public List<AuditLog> getAuditLogsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return auditLogRepository.findByDateRange(startDate, endDate);
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0];
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
