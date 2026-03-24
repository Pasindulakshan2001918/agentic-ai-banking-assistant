package com.agentic.exception;

/**
 * Thrown when validation fails
 */
public class ValidationException extends RuntimeException {
    
    private final String errorCode;
    private final java.util.Map<String, String> fieldErrors;
    
    public ValidationException(String message) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.fieldErrors = new java.util.HashMap<>();
    }
    
    public ValidationException(String message, java.util.Map<String, String> fieldErrors) {
        super(message);
        this.errorCode = "VALIDATION_ERROR";
        this.fieldErrors = fieldErrors != null ? fieldErrors : new java.util.HashMap<>();
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public java.util.Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
