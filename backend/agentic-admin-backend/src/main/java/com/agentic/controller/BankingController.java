package com.agentic.controller;

import com.agentic.dto.ApproveTransactionRequest;
import com.agentic.dto.CreateTransactionRequest;
import com.agentic.dto.RejectTransactionRequest;
import com.agentic.dto.TransactionResponse;
import com.agentic.entity.Transaction;
import com.agentic.service.TransactionService;
import com.agentic.service.AccountService;
import com.agentic.service.UserService;
import com.agentic.service.AuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class BankingController {
    
    private final TransactionService transactionService;
    private final AccountService accountService;
    private final UserService userService;
    private final AuditService auditService;
    
    public BankingController(TransactionService transactionService,
                            AccountService accountService,
                            UserService userService,
                            AuditService auditService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.userService = userService;
        this.auditService = auditService;
    }

    // ==========================================
    // PUBLIC ENDPOINTS (No authentication)
    // ==========================================
    
    @GetMapping("/public/health")
    public ResponseEntity<Map<String, Object>> publicHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "Banking system is operational",
            "timestamp", LocalDateTime.now()
        ));
    }
    
    @GetMapping("/public/info")
    public ResponseEntity<Map<String, String>> publicInfo() {
        return ResponseEntity.ok(Map.of(
            "application", "Agentic Banking System",
            "version", "1.0.0",
            "environment", "development"
        ));
    }

    // ==========================================
    // CREATOR (Maker in Maker-Checker)
    // ==========================================
    
    /**
     * Create a new transaction (CREATOR initiates)
     * Transaction enters PENDING status awaiting APPROVER review
     */
    @PreAuthorize("hasRole('CREATOR')")
    @PostMapping("/banking/transactions")
    public ResponseEntity<Map<String, Object>> createTransaction(
            @Valid @RequestBody CreateTransactionRequest request,
            Authentication auth) {
        try {
            Transaction transaction = transactionService.createTransaction(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                request.getDescription(),
                auth.getName()
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transaction created successfully. Awaiting approval.");
            response.put("transaction", TransactionResponse.fromEntity(transaction));
            response.put("status", transaction.getStatus());
            response.put("createdBy", auth.getName());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get transactions created by authenticated user
     */
    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping("/banking/my-transactions")
    public ResponseEntity<Map<String, Object>> getMyTransactions(
            Authentication auth,
            Pageable pageable) {
        // Get user and their accounts for transactions
        var user = userService.getUserByUsername(auth.getName());
        if (user.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        var accounts = accountService.getUserAccounts(user.get().getId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Your transactions retrieved");
        response.put("username", auth.getName());
        response.put("totalAccounts", accounts.size());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get creator statistics
     */
    @PreAuthorize("hasRole('CREATOR')")
    @GetMapping("/banking/creator/statistics")
    public ResponseEntity<Map<String, Object>> getCreatorStats(Authentication auth) {
        long pendingCount = transactionService.getPendingTransactionCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", auth.getName());
        response.put("statistics", Map.of(
            "pendingApprovals", pendingCount,
            "systemStatus", "Active"
        ));
        
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // APPROVER (Checker in Maker-Checker)
    // ==========================================
    
    /**
     * Get pending transactions awaiting approval
     */
    @PreAuthorize("hasRole('APPROVER')")
    @GetMapping("/banking/approver/pending")
    public ResponseEntity<Map<String, Object>> getPendingTransactions(
            Authentication auth,
            Pageable pageable) {
        List<Transaction> pending = transactionService.getPendingTransactions();
        List<TransactionResponse> responses = pending.stream()
            .map(TransactionResponse::fromEntity)
            .collect(Collectors.toList());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Pending transactions for approval");
        response.put("accessedBy", auth.getName());
        response.put("pendingCount", pending.size());
        response.put("transactions", responses);
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Approve a transaction (APPROVER action)
     * Funds are transferred and balances updated
     */
    @PreAuthorize("hasRole('APPROVER')")
    @PostMapping("/banking/transactions/{transactionId}/approve")
    public ResponseEntity<Map<String, Object>> approveTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody ApproveTransactionRequest request,
            Authentication auth) {
        try {
            Transaction approved = transactionService.approveTransaction(transactionId, auth.getName());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transaction approved successfully");
            response.put("transaction", TransactionResponse.fromEntity(approved));
            response.put("approvedBy", auth.getName());
            response.put("approvedAt", LocalDateTime.now());
            response.put("notes", request.getNotes());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Reject a transaction (APPROVER action)
     */
    @PreAuthorize("hasRole('APPROVER')")
    @PostMapping("/banking/transactions/{transactionId}/reject")
    public ResponseEntity<Map<String, Object>> rejectTransaction(
            @PathVariable Long transactionId,
            @Valid @RequestBody RejectTransactionRequest request,
            Authentication auth) {
        try {
            Transaction rejected = transactionService.rejectTransaction(
                transactionId, auth.getName(), request.getReason());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Transaction rejected successfully");
            response.put("transaction", TransactionResponse.fromEntity(rejected));
            response.put("rejectedBy", auth.getName());
            response.put("rejectedAt", LocalDateTime.now());
            response.put("reason", request.getReason());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    /**
     * Get approver statistics
     */
    @PreAuthorize("hasRole('APPROVER')")
    @GetMapping("/banking/approver/statistics")
    public ResponseEntity<Map<String, Object>> getApproverStats(Authentication auth) {
        long pendingCount = transactionService.getPendingTransactionCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", auth.getName());
        response.put("statistics", Map.of(
            "pendingReview", pendingCount,
            "systemStatus", "Active"
        ));
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // VIEWER (Auditor / Read-only access)
    // ==========================================
    
    /**
     * View all transactions (read-only)
     */
    @PreAuthorize("hasRole('VIEWER')")
    @GetMapping("/banking/viewer/transactions")
    public ResponseEntity<Map<String, Object>> viewAllTransactions(
            Authentication auth,
            Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "All transactions (read-only view)");
        response.put("accessedBy", auth.getName());
        response.put("note", "You have read-only access. Cannot modify transactions.");
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * View audit logs
     */
    @PreAuthorize("hasRole('VIEWER')")
    @GetMapping("/banking/viewer/audit-logs")
    public ResponseEntity<Map<String, Object>> viewAuditLogs(
            Authentication auth,
            Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Audit logs retrieved");
        response.put("accessedBy", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // COMMON ENDPOINTS (Any authenticated user)
    // ==========================================
    
    /**
     * Get transaction details
     */
    @GetMapping("/banking/transactions/{transactionId}")
    public ResponseEntity<?> getTransaction(@PathVariable Long transactionId, Authentication auth) {
        return transactionService.getTransaction(transactionId)
            .map(transaction -> ResponseEntity.ok((Object) TransactionResponse.fromEntity(transaction)))
            .orElse(ResponseEntity.notFound().build());
    }
    
    /**
     * Get dashboard
     */
    @GetMapping("/banking/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication auth) {
        long pendingCount = transactionService.getPendingTransactionCount();
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to your banking dashboard");
        response.put("username", auth.getName());
        response.put("timestamp", LocalDateTime.now());
        response.put("quickStats", Map.of(
            "pendingTransactions", pendingCount,
            "systemStatus", "Operational"
        ));
        
        return ResponseEntity.ok(response);
    }
}