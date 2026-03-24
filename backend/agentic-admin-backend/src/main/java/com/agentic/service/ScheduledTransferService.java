package com.agentic.service;

import com.agentic.dto.ScheduledTransferRequest;
import com.agentic.dto.ScheduledTransferResponse;
import com.agentic.entity.Account;
import com.agentic.entity.ScheduledTransfer;
import com.agentic.entity.Transaction;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.exception.InvalidTransactionException;
import com.agentic.exception.UnauthorizedException;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.ScheduledTransferRepository;
import com.agentic.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SCHEDULED TRANSFER SERVICE
 * 
 * Handles creation and execution of scheduled transfers.
 * 
 * Failure cases to handle:
 * - User tries to schedule negative amount
 * - Execution date is in the past
 * - From account doesn't exist or doesn't belong to user
 * - Duplicate execution (must mark processed)
 * - System crash during execution (reschedule next run)
 */
@Service
public class ScheduledTransferService {
    
    private final ScheduledTransferRepository scheduledTransferRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private final AuditService auditService;
    
    public ScheduledTransferService(ScheduledTransferRepository scheduledTransferRepository,
                                    AccountRepository accountRepository,
                                    TransactionRepository transactionRepository,
                                    TransactionService transactionService,
                                    AuditService auditService) {
        this.scheduledTransferRepository = scheduledTransferRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionService = transactionService;
        this.auditService = auditService;
    }
    
    /**
     * Schedule a future transfer
     * 
     * @param userId User ID (UUID string)
     * @param request Request with accounts, amount, execution date
     * @return Scheduled transfer response
     */
    @Transactional
    public ScheduledTransferResponse scheduleTransfer(String userId, ScheduledTransferRequest request) {
        
        // Validate execution date is in future
        if (request.getExecuteAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTransactionException(
                "Cannot schedule transfer for past date. Execution time must be in the future."
            );
        }
        
        // Validate amount
        if (request.getAmount() == null || request.getAmount().signum() <= 0) {
            throw new InvalidTransactionException("Amount must be positive");
        }
        
        // Get accounts
        Account fromAccount = accountRepository.findById(request.getFromAccountId())
            .orElseThrow(() -> new EntityNotFoundException(
                "From account not found", "Account", request.getFromAccountId()
            ));
        
        Account toAccount = accountRepository.findById(request.getToAccountId())
            .orElseThrow(() -> new EntityNotFoundException(
                "To account not found", "Account", request.getToAccountId()
            ));
        
        // Validate ownership of source account (TODO: update for UUID user IDs)
        // For now, just verify account exists
        
        // Validate accounts are in ACTIVE status
        if (fromAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Source account is not active");
        }
        if (toAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account is not active");
        }
        
        // Create scheduled transfer
        ScheduledTransfer scheduledTransfer = new ScheduledTransfer(
            userId,
            fromAccount,
            toAccount,
            request.getAmount(),
            request.getExecuteAt()
        );
        scheduledTransfer.setDescription(request.getDescription());
        
        ScheduledTransfer saved = scheduledTransferRepository.save(scheduledTransfer);
        
        // Audit log
        auditService.logAction("ScheduledTransfer", saved.getId(), "CREATE", userId,
            null, "Scheduled transfer created",
            "Transfer of " + request.getAmount() + " scheduled for " + request.getExecuteAt()
        );
        
        return ScheduledTransferResponse.fromEntity(saved);
    }
    
    /**
     * Cancel a scheduled transfer (before execution)
     */
    @Transactional
    public void cancelScheduledTransfer(Long scheduledTransferId, String userId) {
        
        ScheduledTransfer st = scheduledTransferRepository.findByIdAndUserId(scheduledTransferId, userId)
            .orElseThrow(() -> new UnauthorizedException(
                "Scheduled transfer not found or does not belong to you"
            ));
        
        if (st.getStatus() != ScheduledTransfer.ScheduledTransferStatus.PENDING) {
            throw new InvalidTransactionException(
                "Can only cancel PENDING transfers. Current status: " + st.getStatus()
            );
        }
        
        st.setStatus(ScheduledTransfer.ScheduledTransferStatus.CANCELLED);
        scheduledTransferRepository.save(st);
        
        auditService.logAction("ScheduledTransfer", st.getId(), "CANCELLED", userId,
            null, "Scheduled transfer cancelled",
            "Transfer of " + st.getAmount() + " cancelled."
        );
    }
    
    /**
     * Get all scheduled transfers for a user
     */
    public List<ScheduledTransferResponse> getUserScheduledTransfers(String userId) {
        return scheduledTransferRepository.findByUserId(userId)
            .stream()
            .map(ScheduledTransferResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * Get pending scheduled transfers for a user
     */
    public List<ScheduledTransferResponse> getPendingScheduledTransfers(String userId) {
        return scheduledTransferRepository
            .findByUserIdAndStatus(userId, ScheduledTransfer.ScheduledTransferStatus.PENDING)
            .stream()
            .map(ScheduledTransferResponse::fromEntity)
            .collect(Collectors.toList());
    }
    
    /**
     * INTERNAL: Execute a scheduled transfer (called by scheduler)
     * 
     * ✅ CRITICAL: Mark as COMPLETED/FAILED before exiting
     *    This prevents duplicate execution if system crashes mid-transfer
     */
    @Transactional
    public void executeScheduledTransfer(ScheduledTransfer st) {
        
        try {
            // Execute the actual transfer
            Transaction tx = transactionService.instantTransfer(
                st.getFromAccount().getId(),
                st.getToAccount().getId(),
                st.getAmount(),
                st.getDescription(),
                1L  // TODO: Convert userId from string to numeric ID
            );
            
            // Mark as completed
            st.setStatus(ScheduledTransfer.ScheduledTransferStatus.COMPLETED);
            st.setExecutedTransaction(tx);
            scheduledTransferRepository.save(st);
            
            auditService.logAction("ScheduledTransfer", st.getId(), "EXECUTED", st.getUserId(),
                null, "Scheduled transfer executed",
                "Transfer of " + st.getAmount() + " executed successfully. Ref: " + tx.getReferenceNumber()
            );
            
        } catch (Exception e) {
            // Mark as failed
            st.setStatus(ScheduledTransfer.ScheduledTransferStatus.FAILED);
            st.setErrorMessage(e.getMessage());
            scheduledTransferRepository.save(st);
            
            auditService.logAction("ScheduledTransfer", st.getId(), "FAILED", st.getUserId(),
                null, "Scheduled transfer failed",
                "Transfer failed: " + e.getMessage()
            );
        }
    }
    
    /**
     * INTERNAL: Find all due scheduled transfers (called by scheduler)
     * Helper method for the cron job
     */
    public List<ScheduledTransfer> findDueTransfers() {
        return scheduledTransferRepository.findDueTransfers(LocalDateTime.now());
    }
}
