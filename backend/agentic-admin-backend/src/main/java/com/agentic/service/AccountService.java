package com.agentic.service;

import com.agentic.entity.Account;
import com.agentic.entity.User;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
public class AccountService {
    
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    
    public AccountService(AccountRepository accountRepository, 
                         UserRepository userRepository,
                         AuditService auditService) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }
    
    /**
     * Create a new account for a user
     */
    @Transactional
    public Account createAccount(Long userId, Account.AccountType accountType, 
                                String createdBy) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(accountType);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setCreatedBy(createdBy);
        account.setUpdatedBy(createdBy);
        
        Account saved = accountRepository.save(account);
        
        // Log audit
        auditService.logAction("Account", saved.getId(), "CREATE", createdBy,
            null, toJsonString(saved), 
            "Account created for user " + user.getUsername());
        
        return saved;
    }
    
    /**
     * Get account by ID
     */
    public Optional<Account> getAccount(Long accountId) {
        return accountRepository.findById(accountId);
    }
    
    /**
     * Get account by account number
     */
    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }
    
    /**
     * Get all accounts for a user
     */
    public List<Account> getUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId);
    }
    
    /**
     * Get account balance
     */
    public BigDecimal getBalance(Long accountId) {
        return accountRepository.findById(accountId)
            .map(Account::getBalance)
            .orElseThrow(() -> new RuntimeException("Account not found"));
    }
    
    /**
     * Update account status
     */
    @Transactional
    public Account updateAccountStatus(Long accountId, Account.AccountStatus newStatus, String updatedBy) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        
        String oldValue = toJsonString(account);
        
        account.setStatus(newStatus);
        account.setUpdatedBy(updatedBy);
        
        Account updated = accountRepository.save(account);
        
        // Log audit
        auditService.logAction("Account", accountId, "UPDATE_STATUS", updatedBy,
            oldValue, toJsonString(updated), 
            "Account status changed to " + newStatus);
        
        return updated;
    }
    
    /**
     * Deactivate an account
     */
    @Transactional
    public Account deactivateAccount(Long accountId, String deactivatedBy) {
        return updateAccountStatus(accountId, Account.AccountStatus.INACTIVE, deactivatedBy);
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private String generateAccountNumber() {
        // Format: ACC-YYYYMMDD-XXXXX
        String timestamp = String.valueOf(System.currentTimeMillis()).substring(0, 10);
        String random = String.valueOf(new Random().nextInt(99999)).formatted("%05d");
        return "ACC-" + timestamp + "-" + random;
    }
    
    private String toJsonString(Account account) {
        return String.format("{\"id\":%d,\"accountNumber\":\"%s\",\"type\":\"%s\",\"status\":\"%s\",\"balance\":%s}",
            account.getId(), account.getAccountNumber(), account.getAccountType(), 
            account.getStatus(), account.getBalance());
    }
}
