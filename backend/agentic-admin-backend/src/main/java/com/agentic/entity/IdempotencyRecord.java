package com.agentic.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * IDEMPOTENCY RECORD
 * 
 * Prevents duplicate processing of identical requests.
 * 
 * Problem this solves:
 * User retries payment request → processed twice → double debit
 * 
 * Solution:
 * 1. Client sends Idempotency-Key header with UUID
 * 2. Store key + response before processing
 * 3. If duplicate key arrives → return stored response
 * 4. Clean up old records after 24 hours
 * 
 * ✅ CRITICAL for production banking
 */
@Entity
@Table(name = "idempotency_records", indexes = {
    @Index(name = "idx_idempotency_key", columnList = "idempotency_key", unique = true),
    @Index(name = "idx_user_id_created", columnList = "user_id, created_at")
})
public class IdempotencyRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Unique request identifier (UUID) from client
     * Format: "Idempotency-Key" HTTP header
     * Example: "550e8400-e29b-41d4-a716-446655440000"
     */
    @Column(nullable = false, unique = true, length = 36)
    private String idempotencyKey;
    
    /**
     * User ID (UUID string from JWT)
     * Used to scope idempotency to specific user
     */
    @Column(nullable = false, length = 36)
    private String userId;
    
    /**
     * HTTP method (POST, PUT, DELETE)
     * Used to determine which requests are idempotent
     */
    @Column(nullable = false)
    private String httpMethod;
    
    /**
     * API endpoint path
     * Example: "/api/customer/transfer"
     */
    @Column(nullable = false)
    private String endpoint;
    
    /**
     * Stored response body (JSON)
     * Returned to client if duplicate request arrives
     */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String responseBody;
    
    /**
     * HTTP response status code
     * Examples: 200, 400, 500
     */
    @Column(nullable = false)
    private Integer responseStatus;
    
    /**
     * When this record was created
     * Used for cleanup (delete records > 24 hours old)
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    
    // ========== CONSTRUCTORS ==========
    
    public IdempotencyRecord() {}
    
    public IdempotencyRecord(String idempotencyKey, String userId, String httpMethod,
                            String endpoint, String responseBody, Integer responseStatus) {
        this.idempotencyKey = idempotencyKey;
        this.userId = userId;
        this.httpMethod = httpMethod;
        this.endpoint = endpoint;
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
    }
    
    // ========== GETTERS & SETTERS ==========
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getIdempotencyKey() {
        return idempotencyKey;
    }
    
    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getHttpMethod() {
        return httpMethod;
    }
    
    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }
    
    public String getResponseBody() {
        return responseBody;
    }
    
    public void setResponseBody(String responseBody) {
        this.responseBody = responseBody;
    }
    
    public Integer getResponseStatus() {
        return responseStatus;
    }
    
    public void setResponseStatus(Integer responseStatus) {
        this.responseStatus = responseStatus;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
