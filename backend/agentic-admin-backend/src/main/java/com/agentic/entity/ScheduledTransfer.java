package com.agentic.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * SCHEDULED TRANSFER ENTITY
 * 
 * Represents a future transfer scheduled by the user.
 * 
 * Flow:
 * 1. User creates scheduled transfer with future date
 * 2. Cron job checks due transfers every minute
 * 3. When execution time arrives → transfer executes automatically
 * 4. Status changes from PENDING to COMPLETED
 * 5. If transfer fails → FAILED with error message
 * 
 * Example:
 * userId: "550e8400-e29b-41d4-a716-446655440000"
 * fromAccountId: 1
 * toAccountId: 2
 * amount: 5000
 * executeAt: 2026-04-22 09:00:00
 * status: PENDING
 */
@Entity
@Table(name = "scheduled_transfers", indexes = {
    @Index(name = "idx_user_id_scheduled", columnList = "user_id"),
    @Index(name = "idx_execute_at_status", columnList = "execute_at, status")
})
public class ScheduledTransfer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * User ID (UUID string from JWT)
     * Owner of this scheduled transfer
     */
    @Column(nullable = false, length = 36)
    private String userId;
    
    /**
     * Source account for transfer
     */
    @ManyToOne
    @JoinColumn(name = "from_account_id", nullable = false)
    private Account fromAccount;
    
    /**
     * Destination account for transfer
     */
    @ManyToOne
    @JoinColumn(name = "to_account_id", nullable = false)
    private Account toAccount;
    
    /**
     * Amount to transfer
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    
    /**
     * Optional description/memo
     */
    @Column(columnDefinition = "TEXT")
    private String description;
    
    /**
     * When this transfer should execute
     * CRITICAL: Used by scheduler job to find due transfers
     */
    @Column(nullable = false)
    private LocalDateTime executeAt;
    
    /**
     * Transfer status
     * PENDING    → Waiting for scheduled time
     * COMPLETED  → Successfully transferred
     * FAILED     → Transfer attempted but failed (see errorMessage)
     * CANCELLED  → User cancelled before execution
     */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private ScheduledTransferStatus status = ScheduledTransferStatus.PENDING;
    
    /**
     * If transfer fails, stores error reason
     * Example: "Insufficient funds"
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    /**
     * Reference to the transaction created upon execution
     * NULL until transfer completes
     */
    @ManyToOne
    @JoinColumn(name = "transaction_id")
    private Transaction executedTransaction;
    
    /**
     * When this scheduled transfer was created
     */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * When this was last updated
     */
    @Column
    private LocalDateTime modifiedAt;
    
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        this.modifiedAt = LocalDateTime.now();
    }
    
    // ========== CONSTRUCTORS ==========
    
    public ScheduledTransfer() {}
    
    public ScheduledTransfer(String userId, Account fromAccount, Account toAccount,
                            BigDecimal amount, LocalDateTime executeAt) {
        this.userId = userId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.executeAt = executeAt;
        this.status = ScheduledTransferStatus.PENDING;
    }
    
    // ========== GETTERS & SETTERS ==========
    
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public Account getFromAccount() {
        return fromAccount;
    }
    
    public void setFromAccount(Account fromAccount) {
        this.fromAccount = fromAccount;
    }
    
    public Account getToAccount() {
        return toAccount;
    }
    
    public void setToAccount(Account toAccount) {
        this.toAccount = toAccount;
    }
    
    public BigDecimal getAmount() {
        return amount;
    }
    
    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public LocalDateTime getExecuteAt() {
        return executeAt;
    }
    
    public void setExecuteAt(LocalDateTime executeAt) {
        this.executeAt = executeAt;
    }
    
    public ScheduledTransferStatus getStatus() {
        return status;
    }
    
    public void setStatus(ScheduledTransferStatus status) {
        this.status = status;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    public Transaction getExecutedTransaction() {
        return executedTransaction;
    }
    
    public void setExecutedTransaction(Transaction executedTransaction) {
        this.executedTransaction = executedTransaction;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getModifiedAt() {
        return modifiedAt;
    }
    
    public void setModifiedAt(LocalDateTime modifiedAt) {
        this.modifiedAt = modifiedAt;
    }
    
    // ========== STATUS ENUM ==========
    
    public enum ScheduledTransferStatus {
        PENDING,     // Waiting to execute
        COMPLETED,   // Successfully executed
        FAILED,      // Execution failed
        CANCELLED    // User cancelled
    }
}
