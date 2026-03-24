package com.agentic.exception;

/**
 * Thrown when concurrent modification conflict occurs
 */
public class ConcurrencyException extends RuntimeException {
    
    private final String errorCode;
    
    public ConcurrencyException(String message) {
        super(message);
        this.errorCode = "CONCURRENCY_ERROR";
    }
    
    public ConcurrencyException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "CONCURRENCY_ERROR";
    }
    
    public String getErrorCode() {
        return errorCode;
    }
}
