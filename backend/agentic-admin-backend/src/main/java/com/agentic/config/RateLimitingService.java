package com.agentic.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Service using Bucket4j
 * ================================================================================
 * Implements per-user rate limiting for sensitive operations:
 * - OTP requests: 5 per minute
 * - Transfer requests: 10 per minute  
 * - API requests: 100 per minute
 * ================================================================================
 */
@Component
public class RateLimitingService {
    
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    
    // Rate limit configurations
    private static final Bandwidth OTP_LIMIT = Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)));
    private static final Bandwidth TRANSFER_LIMIT = Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(1)));
    private static final Bandwidth API_LIMIT = Bandwidth.classic(100, Refill.intervally(100, Duration.ofMinutes(1)));
    
    /**
     * Check if OTP request is allowed for user
     * @return true if allowed, false if rate limited
     */
    public boolean allowOtpRequest(String userId) {
        return checkRateLimit(userId, "otp", OTP_LIMIT);
    }
    
    /**
     * Check if transfer request is allowed
     */
    public boolean allowTransferRequest(String userId) {
        return checkRateLimit(userId, "transfer", TRANSFER_LIMIT);
    }
    
    /**
     * Check if API request is allowed
     */
    public boolean allowApiRequest(String userId) {
        return checkRateLimit(userId, "api", API_LIMIT);
    }
    
    /**
     * Generic rate limit check
     */
    private boolean checkRateLimit(String userId, String operation, Bandwidth bandwidth) {
        String key = userId + ":" + operation;
        Bucket bucket = buckets.computeIfAbsent(key, k -> Bucket4j.builder()
            .addLimit(bandwidth)
            .build()
        );
        
        return bucket.tryConsume(1);
    }
    
    /**
     * Get remaining tokens for operation
     */
    public long getRemainingTokens(String userId, String operation) {
        String key = userId + ":" + operation;
        Bucket bucket = buckets.get(key);
        if (bucket == null) return -1;
        return bucket.getAvailableTokens();
    }
    
    /**
     * Reset rate limit for user (admin only)
     */
    public void resetRateLimit(String userId, String operation) {
        String key = userId + ":" + operation;
        buckets.remove(key);
    }
}
