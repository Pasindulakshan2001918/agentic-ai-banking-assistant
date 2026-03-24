package com.agentic.service;

import com.agentic.entity.Account;
import com.agentic.entity.Transaction;
import com.agentic.entity.User;
import com.agentic.exception.*;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * CENTRALIZED AUTHORIZATION SERVICE
 * Single source of truth for access control
 * Used by ALL services that need to validate ownership
 * 
 * Also provides helper methods compatible with Spring Security @PreAuthorize
 */
@Service
public class SecurityService {
    
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    
    public SecurityService(AccountRepository accountRepository, UserRepository userRepository) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
    }
    
    /**
     * Core validation: Does user own this account?
     * 🔒 Used everywhere an account is accessed
     */
    public void validateAccountOwnership(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Account not found", "Account", accountId));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException(
                "Unauthorized: Account " + accountId + " does not belong to user " + userId);
        }
    }
    
    /**
     * Validate account ownership (takes Account object directly)
     * Used when account is already loaded
     */
    public void validateAccountOwnership(Account account, Long userId) {
        if (account == null || account.getUser() == null) {
            throw new EntityNotFoundException("Account not found", "Account", null);
        }
        
        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException(
                "Unauthorized: Account does not belong to user");
        }
    }
    
    /**
     * Validate user exists and is active
     */
    public User validateUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException(
                "User not found", "User", userId));
        
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException("User account is not active");
        }
        
        return user;
    }
    
    /**
     * Get account only if user owns it
     * Returns the account if authorized, throws if not
     */
    public Account getAccountIfOwner(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Account not found", "Account", accountId));
        
        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Unauthorized account access");
        }
        
        return account;
    }
    
    /**
     * Get transaction and verify that user has access
     * (either created it or it involves their account)
     */
    public void validateTransactionAccess(Transaction transaction, Long userId) {
        boolean isCreator = transaction.getCreatedBy().equals("USER_" + userId);
        boolean isFromOwner = transaction.getFromAccount().getUser().getId().equals(userId);
        boolean isToOwner = transaction.getToAccount().getUser().getId().equals(userId);
        
        if (!isCreator && !isFromOwner && !isToOwner) {
            throw new UnauthorizedException(
                "Unauthorized: User " + userId + " has no access to transaction " + transaction.getId());
        }
    }
    
    /**
     * Batch validate (used before transfers)
     */
    public void validateTransferOwnership(Long fromAccountId, Long toAccountId, Long userId) {
        validateAccountOwnership(fromAccountId, userId);
        
        // toAccount can belong to anyone - just verify it exists
        accountRepository.findById(toAccountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Destination account not found", "Account", toAccountId));
    }
    
    /**
     * Helper method for @PreAuthorize with SpEL
     * Usage: @PreAuthorize("@securityService.canAccessAccount(#accountId, #userId)")
     */
    public boolean canAccessAccount(Long accountId, Long userId) {
        return accountRepository.findById(accountId)
            .map(account -> account.getUser().getId().equals(userId))
            .orElse(false);
    }
    
    /**
     * Helper method for @PreAuthorize with SpEL
     * Usage: @PreAuthorize("@securityService.isAccountActive(#accountId)")
     */
    public boolean isAccountActive(Long accountId) {
        return accountRepository.findById(accountId)
            .map(account -> account.getStatus() == Account.AccountStatus.ACTIVE)
            .orElse(false);
    }
    
    /**
     * Helper method for @PreAuthorize with SpEL
     * Usage: @PreAuthorize("@securityService.isUserActive(#userId)")
     */
    public boolean isUserActive(Long userId) {
        return userRepository.findById(userId)
            .map(user -> user.getStatus() == User.UserStatus.ACTIVE)
            .orElse(false);
    }
}
