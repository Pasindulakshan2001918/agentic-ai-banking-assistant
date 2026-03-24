package com.agentic.exception;

/**
 * Thrown when account has insufficient funds for operation
 */
public class InsufficientFundsException extends RuntimeException {
    
    private final String errorCode;
    private final java.math.BigDecimal available;
    private final java.math.BigDecimal required;
    
    public InsufficientFundsException(String message, java.math.BigDecimal available, java.math.BigDecimal required) {
        super(message);
        this.errorCode = "INSUFFICIENT_FUNDS";
        this.available = available;
        this.required = required;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public java.math.BigDecimal getAvailable() {
        return available;
    }
    
    public java.math.BigDecimal getRequired() {
        return required;
    }
}
