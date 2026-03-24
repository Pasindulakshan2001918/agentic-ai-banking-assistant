package com.agentic.exception;

/**
 * Thrown when transaction exceeds configured limits
 */
public class TransactionLimitExceededException extends RuntimeException {
    
    private final String errorCode;
    private final String limitType;
    private final java.math.BigDecimal limit;
    private final java.math.BigDecimal attempted;
    
    public TransactionLimitExceededException(String message, String limitType, 
                                            java.math.BigDecimal limit, java.math.BigDecimal attempted) {
        super(message);
        this.errorCode = "LIMIT_EXCEEDED";
        this.limitType = limitType;
        this.limit = limit;
        this.attempted = attempted;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getLimitType() {
        return limitType;
    }
    
    public java.math.BigDecimal getLimit() {
        return limit;
    }
    
    public java.math.BigDecimal getAttempted() {
        return attempted;
    }
}
