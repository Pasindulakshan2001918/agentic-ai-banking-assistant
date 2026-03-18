package com.agentic.service;

import com.agentic.entity.Transaction;
import com.agentic.entity.Account;
import com.agentic.repository.TransactionRepository;
import com.agentic.repository.AccountRepository;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class TransactionService {
    
    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AuditService auditService;
    
    public TransactionService(TransactionRepository transactionRepository, 
                             AccountRepository accountRepository,
                             AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.accountRepository = accountRepository;
        this.auditService = auditService;
    }
    
    /**
     * Create a new transaction (MAKER-CHECKER: Initial creation)
     * CREATOR creates the transaction -> stays in PENDING
     * APPROVER reviews and approves/rejects
     */
    @Transactional
    public Transaction createTransaction(Long fromAccountId, Long toAccountId, BigDecimal amount, 
                                        String description, String createdBy) {
        
        // Validate accounts
        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("From account not found"));
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Validate transaction rules
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Source account is not active");
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Destination account is not active");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Amount must be positive");
        }
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient funds");
        }
        
        // Check daily transaction limits
        validateDailyTransactionLimit(fromAccountId, amount);
        
        // Create transaction in PENDING status
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setType(Transaction.TransactionType.TRANSFER);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        transaction.setDescription(description);
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setCreatedBy(createdBy);
        
        Transaction saved = transactionRepository.save(transaction);
        
        // Log audit
        auditService.logAction("Transaction", saved.getId(), "CREATE", createdBy,
            null, toJsonString(saved), "Transaction created and awaiting approval");
        
        return saved;
    }
    
    /**
     * Approve transaction (CHECKER approves)
     * Transfers funds and updates account balances
     */
    @Transactional
    public Transaction approveTransaction(Long transactionId, String approvedBy) {
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new RuntimeException("Only PENDING transactions can be approved");
        }
        
        // Execute the transfer
        Account fromAccount = transaction.getFromAccount();
        Account toAccount = transaction.getToAccount();
        BigDecimal amount = transaction.getAmount();
        
        // Update balances
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        // Update account modification tracking
        fromAccount.setUpdatedBy(approvedBy);
        toAccount.setUpdatedBy(approvedBy);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        // Update transaction status
        transaction.setStatus(Transaction.TransactionStatus.APPROVED);
        transaction.setApprovedAt(LocalDateTime.now());
        transaction.setApprovedBy(approvedBy);
        
        Transaction approved = transactionRepository.save(transaction);
        
        // Log audit
        auditService.logAction("Transaction", approved.getId(), "APPROVE", approvedBy,
            toJsonString(transaction), toJsonString(approved), 
            "Transaction approved - funds transferred from " + fromAccount.getAccountNumber() + 
            " to " + toAccount.getAccountNumber());
        
        return approved;
    }
    
    /**
     * Reject transaction (CHECKER rejects)
     */
    @Transactional
    public Transaction rejectTransaction(Long transactionId, String rejectedBy, String reason) {
        
        Transaction transaction = transactionRepository.findById(transactionId)
            .orElseThrow(() -> new RuntimeException("Transaction not found"));
        
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new RuntimeException("Only PENDING transactions can be rejected");
        }
        
        String oldValue = toJsonString(transaction);
        
        transaction.setStatus(Transaction.TransactionStatus.REJECTED);
        transaction.setRejectedAt(LocalDateTime.now());
        transaction.setRejectedBy(rejectedBy);
        transaction.setRejectionReason(reason);
        
        Transaction rejected = transactionRepository.save(transaction);
        
        // Log audit
        auditService.logAction("Transaction", rejected.getId(), "REJECT", rejectedBy,
            oldValue, toJsonString(rejected), "Transaction rejected - Reason: " + reason);
        
        return rejected;
    }
    
    /**
     * Get all pending transactions (for APPROVER dashboard)
     */
    public List<Transaction> getPendingTransactions() {
        return transactionRepository.findPendingTransactions(Transaction.TransactionStatus.PENDING);
    }
    
    /**
     * Get transaction history for an account
     */
    public Page<Transaction> getAccountTransactionHistory(Long accountId, Pageable pageable) {
        return transactionRepository.findByAccountId(accountId, pageable);
    }
    
    /**
     * Get transaction by ID
     */
    public Optional<Transaction> getTransaction(Long transactionId) {
        return transactionRepository.findById(transactionId);
    }
    
    /**
     * Get transaction by reference number
     */
    public Optional<Transaction> getTransactionByReference(String referenceNumber) {
        return transactionRepository.findByReferenceNumber(referenceNumber);
    }
    
    /**
     * Get transactions within date range
     */
    public List<Transaction> getTransactionsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return transactionRepository.findByDateRange(startDate, endDate);
    }
    
    /**
     * Count pending transactions
     */
    public long getPendingTransactionCount() {
        return transactionRepository.countByStatus(Transaction.TransactionStatus.PENDING);
    }
    
    /**
     * INSTANT TRANSFER - No maker-checker required
     * Direct transfer between accounts (for customer banking)
     * ⚠️ CRITICAL FOR AI: Bypasses approval workflow
     * 🔒 Requires ownership validation in controller
     * 
     * @param fromAccountId - Source account
     * @param toAccountId - Destination account
     * @param amount - Transfer amount
     * @param userId - User performing transfer (for audit)
     * @return Success message
     */
    @Transactional
    public String instantTransfer(Long fromAccountId, Long toAccountId, BigDecimal amount, Long userId) {
        
        // Validate accounts exist
        Account fromAccount = accountRepository.findById(fromAccountId)
            .orElseThrow(() -> new RuntimeException("From account not found"));
        Account toAccount = accountRepository.findById(toAccountId)
            .orElseThrow(() -> new RuntimeException("To account not found"));
        
        // Validate amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Invalid amount: must be positive");
        }
        
        // Validate accounts are active
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Source account is not active");
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Destination account is not active");
        }
        
        // Validate sufficient balance
        if (fromAccount.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("Insufficient balance");
        }
        
        // ⚠️ CRITICAL: This should use optimistic locking to prevent race conditions
        // TODO: Add @Version field to Account entity and handle OptimisticLockingFailureException
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        // Update modification tracking
        String userIdStr = "USER_" + userId;
        fromAccount.setUpdatedBy(userIdStr);
        toAccount.setUpdatedBy(userIdStr);
        
        accountRepository.save(fromAccount);
        accountRepository.save(toAccount);
        
        // Create transaction record (already APPROVED, no pending state)
        Transaction transaction = new Transaction();
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        transaction.setType(Transaction.TransactionType.TRANSFER);
        transaction.setStatus(Transaction.TransactionStatus.APPROVED);
        transaction.setCreatedBy(userIdStr);
        transaction.setApprovedBy(userIdStr);  // Auto-approved
        transaction.setApprovedAt(LocalDateTime.now());
        transaction.setReferenceNumber(generateReferenceNumber());
        transaction.setDescription("Instant transfer");
        transaction.setIsAutoApproved(true);
        
        Transaction saved = transactionRepository.save(transaction);
        
        // Log audit
        auditService.logAction("Transaction", saved.getId(), "INSTANT_TRANSFER", userIdStr,
            null, toJsonString(saved), 
            "Instant transfer completed from " + fromAccount.getAccountNumber() + 
            " to " + toAccount.getAccountNumber() + " - Amount: " + amount);
        
        return "Transfer successful";
    }
    
    // ====== PRIVATE HELPER METHODS ======
    
    private void validateDailyTransactionLimit(Long accountId, BigDecimal amount) {
        LocalDateTime today = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime tomorrow = today.plus(1, ChronoUnit.DAYS);
        
        long dailyCount = transactionRepository.countDailyTransactionsByAccount(accountId, today, tomorrow);
        
        // Limit to 100 transactions per day
        if (dailyCount >= 100) {
            throw new RuntimeException("Daily transaction limit (100) exceeded");
        }
    }
    
    private String generateReferenceNumber() {
        return "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
    
    private String toJsonString(Transaction transaction) {
        return String.format("{\"id\":%d,\"amount\":%s,\"type\":\"%s\",\"status\":\"%s\"}",
            transaction.getId(), transaction.getAmount(), transaction.getType(), transaction.getStatus());
    }
}
