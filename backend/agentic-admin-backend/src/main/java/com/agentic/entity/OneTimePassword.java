package com.agentic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_store", indexes = {
    @Index(name = "idx_otp_user_id", columnList = "user_id"),
    @Index(name = "idx_otp_code", columnList = "otp_code")
})
public class OneTimePassword {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 6)
    private String otpCode;
    
    @Column(nullable = false)
    private String reference; // Transfer reference or action type
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OtpStatus status = OtpStatus.PENDING;
    
    @Column(nullable = false)
    private Integer expiryMinutes = 5;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    private LocalDateTime verifiedAt;
    
    @Column(name = "verified_by")
    private String verifiedBy;
    
    private Integer attemptCount = 0;
    
    public enum OtpStatus {
        PENDING,    // Created but not verified
        VERIFIED,   // Successfully verified
        EXPIRED,    // Verification time expired
        FAILED      // Too many failed attempts
    }
    
    // ===== GETTERS AND SETTERS =====
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public String getOtpCode() {
        return otpCode;
    }
    
    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
    
    public String getReference() {
        return reference;
    }
    
    public void setReference(String reference) {
        this.reference = reference;
    }
    
    public OtpStatus getStatus() {
        return status;
    }
    
    public void setStatus(OtpStatus status) {
        this.status = status;
    }
    
    public Integer getExpiryMinutes() {
        return expiryMinutes;
    }
    
    public void setExpiryMinutes(Integer expiryMinutes) {
        this.expiryMinutes = expiryMinutes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }
    
    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }
    
    public String getVerifiedBy() {
        return verifiedBy;
    }
    
    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }
    
    public Integer getAttemptCount() {
        return attemptCount;
    }
    
    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }
}
