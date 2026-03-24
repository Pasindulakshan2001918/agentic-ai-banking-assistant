package com.agentic.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Card entity — represents a payment card (debit or credit).
 * Supports blocking, contactless, and online payment controls.
 */
@Entity
@Table(name = "cards")
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @NotBlank
    @Column(nullable = false, length = 19)
    private String maskedCardNumber;     // stored as e.g. "**** **** **** 4521"

    @NotBlank
    @Column(nullable = false, length = 4)
    private String lastFourDigits;       // "4521" — for display

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardType cardType;           // DEBIT, CREDIT

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CardStatus status;           // ACTIVE, BLOCKED, EXPIRED

    @Column(nullable = false)
    private boolean onlinePaymentsEnabled;

    @Column(nullable = false)
    private boolean contactlessEnabled;

    @NotNull
    @Column(nullable = false)
    private LocalDate expiryDate;

    @Column(length = 200)
    private String blockReason;          // stored when card is blocked

    @Column
    private LocalDateTime blockedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== ENUMS =====

    public enum CardType {
        DEBIT,
        CREDIT
    }

    public enum CardStatus {
        ACTIVE,
        BLOCKED,
        EXPIRED
    }

    // ===== GETTERS AND SETTERS =====

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public String getMaskedCardNumber() {
        return maskedCardNumber;
    }

    public void setMaskedCardNumber(String maskedCardNumber) {
        this.maskedCardNumber = maskedCardNumber;
    }

    public String getLastFourDigits() {
        return lastFourDigits;
    }

    public void setLastFourDigits(String lastFourDigits) {
        this.lastFourDigits = lastFourDigits;
    }

    public CardType getCardType() {
        return cardType;
    }

    public void setCardType(CardType cardType) {
        this.cardType = cardType;
    }

    public CardStatus getStatus() {
        return status;
    }

    public void setStatus(CardStatus status) {
        this.status = status;
    }

    public boolean isOnlinePaymentsEnabled() {
        return onlinePaymentsEnabled;
    }

    public void setOnlinePaymentsEnabled(boolean onlinePaymentsEnabled) {
        this.onlinePaymentsEnabled = onlinePaymentsEnabled;
    }

    public boolean isContactlessEnabled() {
        return contactlessEnabled;
    }

    public void setContactlessEnabled(boolean contactlessEnabled) {
        this.contactlessEnabled = contactlessEnabled;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getBlockReason() {
        return blockReason;
    }

    public void setBlockReason(String blockReason) {
        this.blockReason = blockReason;
    }

    public LocalDateTime getBlockedAt() {
        return blockedAt;
    }

    public void setBlockedAt(LocalDateTime blockedAt) {
        this.blockedAt = blockedAt;
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
