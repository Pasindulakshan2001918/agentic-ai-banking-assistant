package com.agentic.service.account;

import com.agentic.entity.Account;
import com.agentic.entity.User;
import com.agentic.exception.*;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * ACCOUNT VALIDATOR (Pure stateless validator)
 * 
 * ✅ STRENGTHS:
 * - Stateless component (no service dependencies)
 * - All validation parameters passed in explicitly
 * - No side effects → easy to test deterministically
 * - No circular dependencies
 * 
 * 🔒 BANKING RULES:
 * - Active status required for transactions
 * - Owner verification for authorization
 * - Balance validation
 * - Account type constraints
 */
@Component
public class AccountValidator {
    
    /**
     * Validate account ownership (authorization)
     * Used to verify a user owns an account before transaction
     * 
     * @param account The account to check
     * @param userId The user attempting access
     * @throws UnauthorizedException if user does not own account
     */
    public void validateOwnership(Account account, Long userId) {
        if (account == null) {
            throw new InvalidTransactionException("Account cannot be null");
        }
        if (userId == null) {
            throw new ValidationException("User ID cannot be null");
        }
        
        User accountOwner = account.getUser();
        if (accountOwner == null || !accountOwner.getId().equals(userId)) {
            throw new UnauthorizedException(
                "Unauthorized: Account does not belong to this user"
            );
        }
    }
    
    /**
     * Validate account is in active state
     * Banking rule: Only active accounts can send/receive transfers
     * 
     * @param account The account to validate
     * @param context Description of operation (e.g., "source account", "destination")
     * @throws InvalidTransactionException if account is not active
     */
    public void validateActiveStatus(Account account, String context) {
        if (account == null) {
            throw new InvalidTransactionException("Account cannot be null");
        }
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException(
                "Cannot perform operation: " + context + " is not active. Status: " + account.getStatus()
            );
        }
    }
    
    /**
     * Validate account has sufficient balance
     * 
     * @param account The account to check
     * @param requiredAmount Amount needed
     * @throws InsufficientFundsException if balance is insufficient
     */
    public void validateBalance(Account account, BigDecimal requiredAmount) {
        if (account == null || account.getBalance() == null) {
            throw new ValidationException("Account or balance cannot be null");
        }
        if (requiredAmount == null || requiredAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Required amount must be positive");
        }
        
        if (account.getBalance().compareTo(requiredAmount) < 0) {
            throw new InsufficientFundsException(
                "Insufficient balance: available=" + account.getBalance() + 
                ", required=" + requiredAmount,
                account.getBalance(),
                requiredAmount
            );
        }
    }
    
    /**
     * Validate account is not null and exists
     * 
     * @param account The account to validate
     * @param context Description (e.g., "from", "to")
     * @throws EntityNotFoundException if account is null
     */
    public void validateExists(Account account, String context) {
        if (account == null) {
            throw new EntityNotFoundException(
                context + " account not found",
                "Account",
                null
            );
        }
    }
    
    /**
     * Validate accounts are different (anti-fraud: prevent self-transfers)
     * 
     * @param fromAccount Source account
     * @param toAccount Destination account
     * @throws ValidationException if accounts are the same
     */
    public void validateDifferentAccounts(Account fromAccount, Account toAccount) {
        if (fromAccount != null && toAccount != null && 
            fromAccount.getId().equals(toAccount.getId())) {
            throw new ValidationException("Cannot transfer between the same account");
        }
    }
}
