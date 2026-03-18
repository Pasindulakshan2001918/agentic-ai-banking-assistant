package com.agentic.customer;

import com.agentic.entity.Account;
import com.agentic.repository.AccountRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class CustomerBankingService {
    
    private final AccountRepository accountRepository;
    
    public CustomerBankingService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }
    
    /**
     * Get account balance with ownership validation
     * @param accountId - Account to check
     * @param userId - Logged-in user ID (from token)
     * @return Balance if user owns the account
     * @throws RuntimeException if unauthorized
     */
    public BigDecimal getBalance(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // 🔒 OWNERSHIP VALIDATION
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this account");
        }
        
        return account.getBalance();
    }
    
    /**
     * Get account details with ownership validation
     */
    public Account getAccountDetails(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        // 🔒 OWNERSHIP VALIDATION
        if (!account.getUser().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You do not own this account");
        }
        
        return account;
    }
    
    /**
     * Check if user owns account
     */
    public boolean ownsAccount(Long accountId, Long userId) {
        return accountRepository.findById(accountId)
            .map(account -> account.getUser().getId().equals(userId))
            .orElse(false);
    }
}
