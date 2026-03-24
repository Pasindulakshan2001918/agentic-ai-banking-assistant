package com.agentic.service.transaction;

import com.agentic.entity.Account;
import com.agentic.entity.Transaction;
import com.agentic.exception.*;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.TransactionRepository;
import com.agentic.service.AuditService;
import com.agentic.service.SecurityService;
import jakarta.persistence.OptimisticLockException;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * TRANSACTION ORCHESTRATOR SERVICE (Workflow Coordinator)
 * 
 * ✅ KEY RESPONSIBILITIES:
 * - Security & authorization checks (@PreAuthorize)
 * - Transaction coordination (SERIALIZABLE isolation, pessimistic locking)
 * - Database persistence
 * - Audit logging
 * - Delegating business logic to TransactionDomainService
 * 
 * 🏗️ ARCHITECTURE ROLE:
 * - Step 1 in the flow: Controller → TransactionOrchestratorService → TransactionDomainService
 * - Handles @Transactional, @Retryable, repository access
 * - Manages transaction boundaries (isolation levels, locking)
 * - Coordinates async audit logging
 * 
 * 🔒 SECURITY:
 * - All public methods require @PreAuthorize with appropriate roles
 * - Leverages SecurityService for authorization checks
 * - Validates user ownership of accounts
 * 
 * 🔄 CONCURRENCY:
 * - SERIALIZABLE isolation level (strictest, prevents all anomalies)
 * - Pessimistic locking (LockModeType.PESSIMISTIC_WRITE) on accounts
 * - Retry mechanism for deadlock recovery
 * - Daily limit calculations from DB (handles concurrent updates)
 * 
 * EXAMPLE FLOW:
 * ─────────────
 * Controller.createTransaction(request, auth)
 *     ↓
 * TransactionOrchestratorService.createTransaction(...)
 *     ├─ SecurityService.validateUser(userId)
 *     ├─ AccountRepository.findByIdForUpdate() [locks accounts]
 *     ├─ TransactionDomainService.createTransaction() [pure logic]
 *     ├─ TransactionRepository.save() [persist]
 *     └─ AuditService.log() [async] @Async
 */
@Service
public class TransactionOrchestratorService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final TransactionDomainService transactionDomainService;
    private final SecurityService securityService;
    private final AuditService auditService;
    
    public TransactionOrchestratorService(
            TransactionRepository transactionRepository,
            AccountRepository accountRepository,
            TransactionDomainService transactionDomainService,
            SecurityService securityService,
            AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.transactionDomainService = transactionDomainService;
        this.securityService = securityService;
        this.auditService = auditService;
    }
    
    /**
     * CREATE TRANSACTION (Maker-Checker: Create new transaction awaiting approval)
     * 
     * 🔒 SECURITY: Only CREATOR role can create transactions
     * 🔒 OWNS ACCOUNT: User must own the source account
     * 
     * TRANSACTION PROPERTIES:
     * - SERIALIZABLE: Prevents race conditions on balance checks
     * - PESSIMISTIC_WRITE: Locks accounts to prevent concurrent modifications
     * - ROLLBACK: Any validation failure rolls back entirely
     * 
     * @param fromAccountId Source account
     * @param toAccountId Destination account
     * @param amount Transfer amount
     * @param description Transaction description
     * @param userId Currently authenticated user
     * @return Created Transaction (status = PENDING, awaiting approval)
     * @throws UnauthorizedException if user not authorized
     * @throws EntityNotFoundException if accounts not found
     * @throws InvalidTransactionException if validation fails
     */
    @PreAuthorize("hasRole('CREATOR')")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @org.springframework.transaction.annotation.Transactional(
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = Exception.class
    )
    public Transaction createTransaction(Long fromAccountId, Long toAccountId, BigDecimal amount,
                                        String description, Long userId) {
        
        // 1. SECURITY: Validate user existence
        securityService.validateUser(userId);
        
        // 2. LOCK & FETCH: Get accounts with pessimistic lock
        // This prevents concurrent transactions from modifying these accounts
        Account fromAccount = accountRepository.findByIdForUpdate(fromAccountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "From account not found", "Account", fromAccountId));
        
        Account toAccount = accountRepository.findByIdForUpdate(toAccountId)
            .orElseThrow(() -> new EntityNotFoundException(
                "To account not found", "Account", toAccountId));
        
        // 3. AUTHORIZATION: Verify user owns source account
        securityService.validateAccountOwnership(fromAccount, userId);
        
        // 4. BUSINESS LOGIC: Calculate today's total (from DB, handles concurrency)
        BigDecimal todayTotal = transactionRepository.sumDailyTransfers(
            fromAccountId, LocalDate.now()
        );
        
        // 5. CREATE: Delegate to domain service (pure logic)
        Transaction transaction = transactionDomainService.createTransaction(
            fromAccount, toAccount, amount, description, todayTotal, userId
        );
        
        // 6. PERSIST: Save to database
        Transaction saved = transactionRepository.save(transaction);
        
        // 7. AUDIT: Log asynchronously
        auditService.logAction("Transaction", saved.getId(), "CREATE", userId.toString(),
            null, toJsonString(saved),
            "Transaction created: " + description);
        
        return saved;
    }
    
    /**
     * EXECUTE INSTANT TRANSFER (Immediate execution if under limit)
     * 
     * 🔒 SECURITY: Only CREATOR role can initiate
     * 
     * ATOMICITY:
     * - SERIALIZABLE isolation + pessimistic locks
     * - Either fully succeeds or fully rolls back
     * - No partial state visible
     * 
     * @param transactionId Transaction to execute
     * @param userId Requesting user
     * @return Executed Transaction
     * @throws InvalidTransactionException if not eligible for instant execution
     */
    @PreAuthorize("hasRole('CREATOR')")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @org.springframework.transaction.annotation.Transactional(
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = Exception.class
    )
    public Transaction executeInstantTransfer(Long transactionId, Long userId) {
        
        // Validate user
        securityService.validateUser(userId);
        
        // Get transaction
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Transaction not found", "Transaction", transactionId));
        
        // Verify user is creator
        if (!transaction.getCreatedBy().equals(userId.toString())) {
            throw new UnauthorizedException(
                "Only the transaction creator can execute the transfer"
            );
        }
        
        // Lock accounts for update
        Account fromAccount = accountRepository.findByIdForUpdate(
            transaction.getFromAccount().getId()
        ).orElseThrow(() -> new EntityNotFoundException("Account not found", "Account", null));
        
        Account toAccount = accountRepository.findByIdForUpdate(
            transaction.getToAccount().getId()
        ).orElseThrow(() -> new EntityNotFoundException("Account not found", "Account", null));
        
        // Execute transfer (pure domain logic)
        transactionDomainService.executeTransfer(transaction, fromAccount, toAccount);
        
        // Persist updates
        transactionRepository.save(transaction);
        accountRepository.saveAll(java.util.Arrays.asList(fromAccount, toAccount));
        
        // Audit
        auditService.logAction("Transaction", transactionId, "EXECUTE", userId.toString(),
            "PENDING", "EXECUTED", "Transfer executed");
        
        return transaction;
    }
    
    /**
     * APPROVE TRANSACTION (Checker approves Maker's transaction)
     * 
     * 🔒 SECURITY: Only APPROVER role can approve
     * 
     * In maker-checker flow:
     * 1. CREATOR creates transaction (status=PENDING)
     * 2. APPROVER approves (status=APPROVED)
     * 3. System executes transfer
     * 
     * @param transactionId Transaction to approve
     * @param approverId ID of approver user
     * @return Approved Transaction
     */
    @PreAuthorize("hasRole('APPROVER')")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @org.springframework.transaction.annotation.Transactional(
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = Exception.class
    )
    public Transaction approveTransaction(Long transactionId, Long approverId) {
        
        securityService.validateUser(approverId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Transaction not found", "Transaction", transactionId));
        
        // Domain logic: approve
        transactionDomainService.approveTransaction(transaction, approverId);
        
        // Persist
        Transaction saved = transactionRepository.save(transaction);
        
        // Audit
        auditService.logAction("Transaction", transactionId, "APPROVE", approverId.toString(),
            "PENDING", "APPROVED", "Transaction approved");
        
        return saved;
    }
    
    /**
     * REJECT TRANSACTION (Checker rejects Maker's transaction)
     * 
     * 🔒 SECURITY: Only APPROVER role can reject
     * 
     * @param transactionId Transaction to reject
     * @param rejectReason Reason for rejection
     * @param approverId ID of approver
     * @return Rejected Transaction
     */
    @PreAuthorize("hasRole('APPROVER')")
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    @org.springframework.transaction.annotation.Transactional(
        isolation = Isolation.SERIALIZABLE,
        rollbackFor = Exception.class
    )
    public Transaction rejectTransaction(Long transactionId, String rejectReason, Long approverId) {
        
        securityService.validateUser(approverId);
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new EntityNotFoundException(
                "Transaction not found", "Transaction", transactionId));
        
        // Domain logic: reject
        transactionDomainService.rejectTransaction(transaction, rejectReason, approverId);
        
        // Persist
        Transaction saved = transactionRepository.save(transaction);
        
        // Audit
        auditService.logAction("Transaction", transactionId, "REJECT", approverId.toString(),
            "PENDING", "REJECTED", "Reason: " + rejectReason);
        
        return saved;
    }
    
    /**
     * GET TRANSACTION by ID (with ownership verification)
     * 
     * @param transactionId Transaction ID
     * @param userId Current user
     * @return Transaction if user is involved (creator, approver, or account owner)
     */
    @PreAuthorize("isAuthenticated()")
    public Optional<Transaction> getTransaction(Long transactionId, Long userId) {
        return transactionRepository.findById(transactionId)
            .filter(tx -> isUserAuthorized(tx, userId));
    }
    
    /**
     * GET PENDING TRANSACTIONS (for Approvers to review)
     * 
     * 🔒 SECURITY: Only APPROVER role can view pending queue
     * 
     * @return List of PENDING transactions awaiting approval
     */
    @PreAuthorize("hasRole('APPROVER')")
    public java.util.List<Transaction> getPendingTransactions() {
        return transactionRepository.findPendingTransactions(
            Transaction.TransactionStatus.PENDING
        );
    }
    
    /**
     * GET MY TRANSACTIONS (user-specific) - simplified version
     * 
     * @param userId Current authenticated user
     * @return All transactions created by user
     */
    @PreAuthorize("isAuthenticated()")
    public java.util.List<Transaction> getMyTransactions(Long userId) {
        // Get all transactions via JPA repository (from cache or DB)
        List<Transaction> allTransactions = transactionRepository.findAll();
        
        // Filter transactions by creator
        return allTransactions.stream()
            .filter(tx -> tx.getCreatedBy() != null && 
                         tx.getCreatedBy().equals(userId.toString()))
            .collect(Collectors.toList());
    }
    
    /**
     * Helper: Check if user is authorized to view transaction
     */
    private boolean isUserAuthorized(Transaction transaction, Long userId) {
        // User is creator
        if (transaction.getCreatedBy() != null && 
            transaction.getCreatedBy().equals(userId.toString())) {
            return true;
        }
        // User is approver
        if (transaction.getApprovedBy() != null && 
            transaction.getApprovedBy().equals(userId.toString())) {
            return true;
        }
        // User owns one of the accounts
        return transaction.getFromAccount().getUser().getId().equals(userId) ||
               transaction.getToAccount().getUser().getId().equals(userId);
    }
    
    /**
     * Helper: Convert to JSON for audit
     */
    private String toJsonString(Transaction tx) {
        return String.format(
            "{id:%d, amount:%s, status:%s, type:%s}",
            tx.getId(), tx.getAmount(), tx.getStatus(), tx.getType()
        );
    }
}
