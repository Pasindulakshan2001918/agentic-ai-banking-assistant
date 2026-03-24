package com.agentic.config;

import org.slf4j.MDC;

/**
 * CORRELATION ID HOLDER
 * 
 * Utility class to access correlation ID from anywhere in the application
 * Uses SLF4J MDC (Mapped Diagnostic Context) under the hood
 */
public class CorrelationIdHolder {
    
    private static final String CORRELATION_ID_MDC = "correlationId";
    
    /**
     * Get the current correlation ID
     * Returns null if not in HTTP request context
     */
    public static String getCorrelationId() {
        return MDC.get(CORRELATION_ID_MDC);
    }
    
    /**
     * Set correlation ID (usually done by ObservabilityFilter)
     */
    public static void setCorrelationId(String correlationId) {
        if (correlationId != null) {
            MDC.put(CORRELATION_ID_MDC, correlationId);
        }
    }
    
    /**
     * Clear correlation ID
     */
    public static void clear() {
        MDC.remove(CORRELATION_ID_MDC);
    }
}
