package com.agentic.exception;

/**
 * Thrown when transaction operation is invalid or violates business rules
 */
public class InvalidTransactionException extends RuntimeException {
    
    private final String errorCode;
    private final String transactionId;
    
    public InvalidTransactionException(String message, String transactionId) {
        super(message);
        this.errorCode = "INVALID_TRANSACTION";
        this.transactionId = transactionId;
    }
    
    public InvalidTransactionException(String message) {
        super(message);
        this.errorCode = "INVALID_TRANSACTION";
        this.transactionId = null;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getTransactionId() {
        return transactionId;
    }
}
