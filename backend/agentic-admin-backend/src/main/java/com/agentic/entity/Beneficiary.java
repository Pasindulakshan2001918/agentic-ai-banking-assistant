package com.agentic.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "beneficiaries", indexes = {
    @Index(name = "idx_beneficiary_user_id", columnList = "user_id"),
    @Index(name = "idx_account_id", columnList = "account_id")
})
public class Beneficiary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // Owner of this beneficiary list
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account; // Target account for transfers
    
    @Column(nullable = false)
    private String nickname; // e.g., "Nimal", "Mom", "Office"
    
    @Column(nullable = false)
    private String accountHolderName;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeneficiaryStatus status = BeneficiaryStatus.ACTIVE;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    public enum BeneficiaryStatus {
        ACTIVE,     // Can transfer to this beneficiary
        SUSPENDED,  // Temporarily suspended
        INACTIVE    // Deleted/Archived
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
    
    public Account getAccount() {
        return account;
    }
    
    public void setAccount(Account account) {
        this.account = account;
    }
    
    public String getNickname() {
        return nickname;
    }
    
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    
    public String getAccountHolderName() {
        return accountHolderName;
    }
    
    public void setAccountHolderName(String accountHolderName) {
        this.accountHolderName = accountHolderName;
    }
    
    public BeneficiaryStatus getStatus() {
        return status;
    }
    
    public void setStatus(BeneficiaryStatus status) {
        this.status = status;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
