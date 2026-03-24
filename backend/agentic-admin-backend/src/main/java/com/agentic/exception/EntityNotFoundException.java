package com.agentic.exception;

/**
 * Thrown when an entity is not found
 */
public class EntityNotFoundException extends RuntimeException {
    
    private final String errorCode;
    private final String entityType;
    private final Long entityId;
    
    public EntityNotFoundException(String message, String entityType, Long entityId) {
        super(message);
        this.errorCode = "NOT_FOUND";
        this.entityType = entityType;
        this.entityId = entityId;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getEntityType() {
        return entityType;
    }
    
    public Long getEntityId() {
        return entityId;
    }
}
