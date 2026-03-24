package com.agentic.service;

import com.agentic.config.TransferConfig;
import com.agentic.entity.Account;
import com.agentic.exception.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * PURE TRANSACTION VALIDATOR
 * Stateless, enforces business rules for transfers
 * No dependencies on services → no circular dependencies
 * Daily total passed in as parameter → handles concurrency safely
 * Uses externalized configuration from TransferConfig
 */
@Component
public class TransactionValidator {
    
    private final TransferConfig transferConfig;
    
    public TransactionValidator(TransferConfig transferConfig) {
        this.transferConfig = transferConfig;
    }
    
    /**
     * 🔒 PURE VALIDATION: All parameters explicitly passed
     * No side effects, deterministic, easy to test
     * Validates on all inputs including null checks and configuration limits
     * 
     * @param fromAccount source account (must not be null)
     * @param toAccount destination account (must not be null)
     * @param amount transfer amount (must be positive)
     * @param dailyTotal pre-calculated sum of today's approved transfers (must not be null)
     * @throws ValidationException if any parameter is invalid
     * @throws InvalidTransactionException if transaction is invalid
     * @throws InsufficientFundsException if balance is insufficient
     * @throws TransactionLimitExceededException if limit is exceeded
     */
    public void validateTransfer(Account fromAccount, Account toAccount, BigDecimal amount, BigDecimal dailyTotal) {
        
        // 0. Null checks for critical entities
        if (fromAccount == null) {
            throw new InvalidTransactionException("Source account cannot be null");
        }
        if (toAccount == null) {
            throw new InvalidTransactionException("Destination account cannot be null");
        }
        if (amount == null) {
            throw new ValidationException("Transfer amount cannot be null");
        }
        if (dailyTotal == null) {
            throw new ValidationException("Daily total cannot be null");
        }
        
        // 1. Account status checks
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Source account is not active");
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account is not active");
        }
        
        // 2. Amount validation
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Transfer amount must be positive");
        }
        
        // 3. Sufficient balance check
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient balance: available=" + fromAccount.getBalance() + 
                ", required=" + amount,
                fromAccount.getBalance(), amount);
        }
        
        // 4. Daily limit check (using externalized config)
        // dailyTotal is pre-calculated from DB, so it's consistent under concurrency
        BigDecimal dailyLimit = transferConfig.getDaily().getLimit();
        
        // Validate transfer config is properly configured (positive limit)
        if (dailyLimit == null || dailyLimit.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Daily transfer limit is not properly configured");
        }
        
        if (dailyTotal.add(amount).compareTo(dailyLimit) > 0) {
            throw new TransactionLimitExceededException(
                "Daily transfer limit exceeded: " + dailyLimit,
                "DAILY_TRANSFER_LIMIT",
                dailyLimit,
                dailyTotal.add(amount));
        }
        
        // 5. Same account check
        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new ValidationException("Cannot transfer to the same account");
        }
        
        // 6. Instant transfer limit validation (if applicable)
        BigDecimal instantLimit = transferConfig.getInstant().getLimit();
        if (instantLimit != null && instantLimit.compareTo(BigDecimal.ZERO) > 0) {
            // This is informational - used by determineTransferType
            // Not a blocking validation, just policy enforcement
        }
    }
    
    /**
     * Determine if transfer requires approval or can be instant
     * POLICY: Uses instant transfer limit from externalized config
     */
    public TransferType determineTransferType(BigDecimal amount) {
        if (amount.compareTo(transferConfig.getInstant().getLimit()) <= 0) {
            return TransferType.INSTANT;
        } else {
            return TransferType.MAKER_CHECKER;
        }
    }
    
    /**
     * Policy: When to use instant vs maker-checker
     */
    public enum TransferType {
        INSTANT,        // Immediate execution
        MAKER_CHECKER   // Requires approval
    }
}
