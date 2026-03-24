package com.agentic.service.account;

import com.agentic.dto.BalanceResponse;
import com.agentic.entity.Account;
import com.agentic.entity.User;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.UserRepository;
import com.agentic.service.AuditService;
import jakarta.transaction.Transactional;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * ACCOUNT SERVICE (Domain Service)
 * 
 * ✅ RESPONSIBILITIES:
 * - Account lifecycle (create, read, update, delete)
 * - Account queries with ownership validation
 * - Account balance retrieval with security checks
 * - Account status management
 * 
 * 🔒 SECURITY:
 * - All public methods enforced with @PreAuthorize
 * - User ownership validated before returning account data
 * - Role-based access control on sensitive operations
 * 
 * ARCHITECTURE NOTES:
 * - Thin service (doesn't do transaction logic)
 * - Delegates validation to AccountValidator
 * - Coordinates with AccountRepository
 * - Logs changes via AuditService
 */
@Service
public class AccountDomainService {
    
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final AccountValidator accountValidator;
    
    public AccountDomainService(AccountRepository accountRepository,
                         UserRepository userRepository,
                         AuditService auditService,
                         AccountValidator accountValidator) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.accountValidator = accountValidator;
    }
    
    /**
     * Create a new account for a user (ADMIN only)
     * 
     * 🔒 Security: Requires ADMIN role to create accounts
     * 
     * @param userId Owner of the account
     * @param accountType Type of account (SAVINGS, CHECKING, etc.)
     * @param createdBy Username of admin creating account
     * @return Newly created Account
     * @throws EntityNotFoundException if user not found
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Transactional
    public Account createAccount(Long userId, Account.AccountType accountType, 
                                String createdBy) {
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("User not found", "User", userId));
        
        Account account = new Account();
        account.setUser(user);
        account.setAccountType(accountType);
        account.setAccountNumber(generateAccountNumber());
        account.setBalance(BigDecimal.ZERO);
        account.setStatus(Account.AccountStatus.ACTIVE);
        account.setCreatedBy(createdBy);
        account.setUpdatedBy(createdBy);
        
        Account saved = accountRepository.save(account);
        
        // Audit log
        auditService.logAction("Account", saved.getId(), "CREATE", createdBy,
            null, toJsonString(saved), 
            "Account created for user " + user.getUsername());
        
        return saved;
    }
    
    /**
     * Get account by ID with ownership validation
     * 
     * 🔒 Security: User can only access their own accounts
     * 
     * @param accountId ID of account
     * @param userId Current authenticated user
     * @return Account if user owns it
     * @throws EntityNotFoundException if account doesn't exist
     */
    @PreAuthorize("isAuthenticated()")
    public Optional<Account> getAccountForUser(Long accountId, Long userId) {
        return accountRepository.findById(accountId)
            .filter(account -> {
                accountValidator.validateOwnership(account, userId);
                return true;
            });
    }
    
    /**
     * Get account by account number (for receiving transfers)
     * 
     * ⚠️ Note: Account number is somewhat public (used in transfers)
     * but the account itself cannot be modified by non-owners
     * 
     * @param accountNumber Account number
     * @return Account if exists
     */
    public Optional<Account> getAccountByNumber(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber);
    }
    
    /**
     * Get all accounts belonging to a user
     * 
     * 🔒 Security: Requires authentication
     * 
     * @param userId User ID to retrieve accounts for
     * @return List of accounts owned by user
     */
    @PreAuthorize("isAuthenticated()")
    public List<Account> getUserAccounts(Long userId) {
        return accountRepository.findByUserId(userId);
    }
    
    /**
     * Get account balance with security checks
     * 
     * 🔒 Security: User can only check their own account balance
     * 
     * @param accountId Account ID
     * @param userId Current authenticated user
     * @return Balance response
     * @throws EntityNotFoundException if account not found
     */
    @PreAuthorize("isAuthenticated()")
    public BalanceResponse getBalance(Long accountId, Long userId) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Account not found", "Account", accountId));
        
        // Validate ownership
        accountValidator.validateOwnership(account, userId);
        
        return new BalanceResponse(
            account.getId(),
            account.getAccountNumber(),
            account.getBalance(),
            "USD",  // Default currency - can be parametrized
            account.getStatus().name()
        );
    }
    
    /**
     * Update account status (ADMIN only)
     * 
     * 🔒 Security: Only ADMIN can change account status
     * 
     * @param accountId Account to update
     * @param newStatus New status
     * @param updatedBy Admin performing update
     * @return Updated Account
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Transactional
    public Account updateAccountStatus(Long accountId, Account.AccountStatus newStatus, 
                                      String updatedBy) {
        Account account = accountRepository.findById(accountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Account not found", "Account", accountId));
        
        Account.AccountStatus oldStatus = account.getStatus();
        account.setStatus(newStatus);
        account.setUpdatedBy(updatedBy);
        
        Account updated = accountRepository.save(account);
        
        // Audit log
        auditService.logAction("Account", accountId, "UPDATE_STATUS", updatedBy,
            oldStatus.name(), newStatus.name(), 
            "Account status changed from " + oldStatus + " to " + newStatus);
        
        return updated;
    }
    
    /**
     * Debit account (internal use by TransactionService)
     * Used during fund transfer execution
     * 
     * @param account Account to debit
     * @param amount Amount to debit
     * @throws Validation exception if balance insufficient
     */
    public void debit(Account account, BigDecimal amount) {
        accountValidator.validateBalance(account, amount);
        account.setBalance(account.getBalance().subtract(amount));
    }
    
    /**
     * Credit account (internal use by TransactionService)
     * Used during fund transfer execution
     * 
     * @param account Account to credit
     * @param amount Amount to credit
     */
    public void credit(Account account, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Credit amount must be positive");
        }
        account.setBalance(account.getBalance().add(amount));
    }
    
    /**
     * Generate unique account number (production: use proper algorithm)
     * Current: Random 10-digit number
     */
    private String generateAccountNumber() {
        return String.valueOf(10_000_000_000L + new Random().nextLong(9_000_000_000L));
    }
    
    /**
     * Convert account to JSON string for audit logging
     */
    private String toJsonString(Account account) {
        return String.format(
            "{id:%d, accountNumber:\"%s\", type:\"%s\", status:\"%s\", balance:%s}",
            account.getId(), account.getAccountNumber(), account.getAccountType(),
            account.getStatus(), account.getBalance()
        );
    }
}
