package com.agentic.service.transaction;

import com.agentic.entity.Account;
import com.agentic.entity.SpendingCategory;
import com.agentic.entity.Transaction;
import com.agentic.exception.ValidationException;
import com.agentic.service.TransactionValidator;
import com.agentic.service.account.AccountValidator;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * TRANSACTION DOMAIN SERVICE (Pure Business Logic)
 * 
 * ✅ KEY CHARACTERISTICS:
 * - Stateless service
 * - No repository/database dependencies
 * - NO side effects → completely testable in isolation
 * - Only handles DOMAIN RULES, not persistence or security
 * - All inputs passed in explicitly (no state manipulation)
 * 
 * 🏗️ ARCHITECTURE ROLE:
 * - Step 2 in the flow: TransactionOrchestratorService → TransactionDomainService → Execute
 * - Pure computation: given inputs → produces domain model (Transaction entity)
 * - Validators are injected but used only for rule checking
 * - No database access, no external calls
 * 
 * 📊 BANKING LOGIC:
 * - Create transaction with PENDING status (unexecuted)
 * - Determine transfer type (INSTANT vs MAKER_CHECKER) based on amount
 * - Execute transfer (debit source, credit destination)
 * - Build fully configured Transaction entity
 * 
 * EXAMPLE USAGE:
 * ─────────────
 * // In TransactionOrchestratorService (orchestrator layer):
 * 
 * Transaction transaction = domainService.createTransaction(
 *     fromAccount,                    // Already validated by orchestrator
 *     toAccount,
 *     amount,
 *     description,
 *     dailyTotalFromDB,              // Pre-fetched from DB
 *     userId
 * );
 * 
 * // At this point: Transaction is valid, ready to persist
 * // Orchestrator handles @Transactional, persistence, audit logging
 */
@Service
public class TransactionDomainService {
    
    private final TransactionValidator transactionValidator;
    private final AccountValidator accountValidator;
    
    public TransactionDomainService(TransactionValidator transactionValidator,
                                    AccountValidator accountValidator) {
        this.transactionValidator = transactionValidator;
        this.accountValidator = accountValidator;
    }
    
    /**
     * CREATE TRANSACTION (domain operation)
     * 
     * Pure function: given accounts and amount → returns valid Transaction entity
     * Does NOT persist to database (orchestrator handles that)
     * 
     * GUARANTEES (post-condition):
     * - Transaction has unique idempotency key
     * - TransactionStatus = PENDING (not executed yet)
     * - Type determined by business rule (instant vs maker-checker)
     * - Accounts have NOT been modified (no side effects)
     * 
     * @param fromAccount Source account (must be validated already)
     * @param toAccount Destination account (must be validated already)
     * @param amount Transfer amount (must be positive)
     * @param description Transaction description
     * @param dailyTotal Pre-calculated total of today's transfers (from DB query)
     * @param userId Currently authenticated user
     * @return Valid Transaction entity ready for persistence
     * @throws ValidationException if any business rule is violated
     */
    public Transaction createTransaction(Account fromAccount,
                                        Account toAccount,
                                        BigDecimal amount,
                                        String description,
                                        BigDecimal dailyTotal,
                                        Long userId) {
        
        // Validate all business rules
        transactionValidator.validateTransfer(fromAccount, toAccount, amount, dailyTotal);
        
        // Determine transfer type (TRANSFER if under limit, else TRANSFER with PENDING)
        Transaction.TransactionType transferType = Transaction.TransactionType.TRANSFER;
        
        // Create domain model (entity, not yet persisted)
        Transaction transaction = new Transaction();
        
        // Identifiable & idempotency
        transaction.setIdempotencyKey(UUID.randomUUID().toString());
        
        // Accounts & amounts
        transaction.setFromAccount(fromAccount);
        transaction.setToAccount(toAccount);
        transaction.setAmount(amount);
        
        // Metadata
        transaction.setDescription(description);
        transaction.setType(transferType);
        transaction.setStatus(Transaction.TransactionStatus.PENDING);
        
        // Auto-categorize the transaction based on description and type
        transaction.setCategory(inferCategory(description, transferType));
        
        // Creator
        transaction.setCreatedBy(userId.toString());
        
        return transaction;
    }
    
    /**
     * EXECUTE TRANSFER (pure domain logic, no persistence)
     * 
     * Given validated accounts and amount:
     * 1. Perform debit on source account
     * 2. Perform credit on destination account
     * 3. Mark transaction as COMPLETED
     * 
     * ⚠️ NOTE: This MUTATES account objects in memory
     * Persistence and transaction boundaries handled by orchestrator
     * 
     * @param transaction The transaction to execute
     * @param fromAccount Source account (will be modified)
     * @param toAccount Destination account (will be modified)
     */
    public void executeTransfer(Transaction transaction, Account fromAccount, Account toAccount) {
        
        // Preconditions
        if (transaction.getStatus() != Transaction.TransactionStatus.PENDING) {
            throw new ValidationException(
                "Cannot execute transaction with status: " + transaction.getStatus()
            );
        }
        
        BigDecimal amount = transaction.getAmount();
        
        // Validate again (safety check for concurrent changes)
        accountValidator.validateBalance(fromAccount, amount);
        accountValidator.validateActiveStatus(fromAccount, "source account");
        accountValidator.validateActiveStatus(toAccount, "destination account");
        
        // Execute transfer (modifies accounts in memory)
        fromAccount.setBalance(fromAccount.getBalance().subtract(amount));
        toAccount.setBalance(toAccount.getBalance().add(amount));
        
        // Mark transaction as completed
        transaction.setStatus(Transaction.TransactionStatus.COMPLETED);
    }
    
    /**
     * APPROVE TRANSACTION (for maker-checker flow)
     * 
     * Only applicable for PENDING transactions
     * 
     * @param transaction Transaction to approve
     * @param approverId ID of approver
     * @throws ValidationException if not approvable
     */
    public void approveTransaction(Transaction transaction, Long approverId) {
        if (!Transaction.TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new ValidationException(
                "Can only approve PENDING transactions. Current status: " + transaction.getStatus()
            );
        }
        
        transaction.setStatus(Transaction.TransactionStatus.APPROVED);
        transaction.setApprovedBy(approverId.toString());
        transaction.setApprovedAt(LocalDateTime.now(ZoneOffset.UTC));
    }
    
    /**
     * REJECT TRANSACTION (for maker-checker flow)
     * 
     * Only applicable for PENDING transactions
     * 
     * @param transaction Transaction to reject
     * @param rejectReason Reason for rejection
     * @param approverId ID of person rejecting
     */
    public void rejectTransaction(Transaction transaction, String rejectReason, Long approverId) {
        if (!Transaction.TransactionStatus.PENDING.equals(transaction.getStatus())) {
            throw new ValidationException(
                "Can only reject PENDING transactions. Current status: " + transaction.getStatus()
            );
        }
        
        transaction.setStatus(Transaction.TransactionStatus.REJECTED);
        transaction.setRejectionReason(rejectReason);
        transaction.setRejectedBy(approverId.toString());
        transaction.setRejectedAt(LocalDateTime.now(ZoneOffset.UTC));
    }
    
    /**
     * AUTO-CATEGORIZE TRANSACTION
     * 
     * Infers spending category based on transaction description and type.
     * Uses pattern matching on common keywords.
     * 
     * @param description Transaction description
     * @param type Transaction type
     * @return Inferred SpendingCategory, defaults to OTHER
     */
    private SpendingCategory inferCategory(String description, Transaction.TransactionType type) {
        if (description == null) return SpendingCategory.OTHER;
        String d = description.toLowerCase();

        if (d.contains("salary") || d.contains("payroll")) 
            return SpendingCategory.SALARY;
        if (d.contains("food") || d.contains("restaurant") ||
            d.contains("grocery") || d.contains("cafe"))    
            return SpendingCategory.FOOD;
        if (d.contains("uber") || d.contains("pickme") ||
            d.contains("fuel") || d.contains("transport"))  
            return SpendingCategory.TRANSPORT;
        if (d.contains("ceb") || d.contains("water") ||
            d.contains("electricity") || d.contains("bill")) 
            return SpendingCategory.UTILITIES;
        if (d.contains("amazon") || d.contains("shop") ||
            d.contains("mall"))                              
            return SpendingCategory.SHOPPING;
        if (type == Transaction.TransactionType.TRANSFER)                
            return SpendingCategory.TRANSFER;

        return SpendingCategory.OTHER;
    }
}
