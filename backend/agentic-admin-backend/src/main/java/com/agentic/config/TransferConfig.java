package com.agentic.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

/**
 * TRANSFER CONFIGURATION
 * Externalizes transfer limits and policies
 * Configurable via application.yml / application.properties
 * 🔒 Validated on load to prevent invalid configurations
 * 
 * Usage in application.yml:
 * transfer:
 *   instant:
 *     limit: 250000
 *   daily:
 *     limit: 5000000
 *   daily-transaction-count-limit: 100
 */
@Component
@ConfigurationProperties(prefix = "transfer")
@Validated
public class TransferConfig {
    
    @Valid
    private Instant instant = new Instant();
    
    @Valid
    private Daily daily = new Daily();
    
    private int dailyTransactionCountLimit = 100;
    
    // ===== GETTERS AND SETTERS =====
    
    public Instant getInstant() {
        return instant;
    }
    
    public void setInstant(Instant instant) {
        if (instant != null) {
            this.instant = instant;
        }
    }
    
    public Daily getDaily() {
        return daily;
    }
    
    public void setDaily(Daily daily) {
        if (daily != null) {
            this.daily = daily;
        }
    }
    
    public int getDailyTransactionCountLimit() {
        return dailyTransactionCountLimit;
    }
    
    public void setDailyTransactionCountLimit(int dailyTransactionCountLimit) {
        if (dailyTransactionCountLimit > 0) {
            this.dailyTransactionCountLimit = dailyTransactionCountLimit;
        }
    }
    
    /**
     * Instant transfer configuration
     * 🔒 Limit must be positive
     */
    public static class Instant {
        @Positive(message = "Instant transfer limit must be positive")
        private BigDecimal limit = new BigDecimal("250000");
        
        public BigDecimal getLimit() {
            return limit;
        }
        
        public void setLimit(BigDecimal limit) {
            // Protect against negative or zero limits
            if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
                this.limit = limit;
            }
        }
    }
    
    /**
     * Daily transfer configuration
     * 🔒 Limit must be positive
     */
    public static class Daily {
        @Positive(message = "Daily transfer limit must be positive")
        private BigDecimal limit = new BigDecimal("5000000");
        
        public BigDecimal getLimit() {
            return limit;
        }
        
        public void setLimit(BigDecimal limit) {
            // Protect against negative or zero limits
            if (limit != null && limit.compareTo(BigDecimal.ZERO) > 0) {
                this.limit = limit;
            }
        }
    }
}
