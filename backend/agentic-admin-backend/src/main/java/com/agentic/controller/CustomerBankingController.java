package com.agentic.controller;

import com.agentic.customer.CustomerBankingService;
import com.agentic.dto.TransferRequest;
import com.agentic.entity.Account;
import com.agentic.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerBankingController {
    
    private final CustomerBankingService customerService;
    private final TransactionService transactionService;
    
    public CustomerBankingController(CustomerBankingService customerService,
                                     TransactionService transactionService) {
        this.customerService = customerService;
        this.transactionService = transactionService;
    }
    
    /**
     * Get account balance (customer can only see their own)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/balance/{accountId}")
    public ResponseEntity<Map<String, Object>> getBalance(
            @PathVariable Long accountId,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            BigDecimal balance = customerService.getBalance(accountId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("accountId", accountId);
            response.put("balance", balance);
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get account details (with ownership check)
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/account/{accountId}")
    public ResponseEntity<?> getAccountDetails(
            @PathVariable Long accountId,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            Account account = customerService.getAccountDetails(accountId, userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", account.getId());
            response.put("accountNumber", account.getAccountNumber());
            response.put("type", account.getAccountType());
            response.put("balance", account.getBalance());
            response.put("status", account.getStatus());
            response.put("currency", account.getCurrency());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Instant transfer between accounts (with ownership validation)
     * CRITICAL FOR AI: Direct transfer without approval workflow
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/transfer")
    public ResponseEntity<Map<String, Object>> instantTransfer(
            @Valid @RequestBody TransferRequest request,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            
            // 🔒 Validate user owns the FROM account
            if (!customerService.ownsAccount(request.getFromAccountId(), userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You do not own the source account"));
            }
            
            // Execute instant transfer (bypasses maker-checker)
            String result = transactionService.instantTransfer(
                request.getFromAccountId(),
                request.getToAccountId(),
                request.getAmount(),
                userId
            );
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", result);
            response.put("fromAccountId", request.getFromAccountId());
            response.put("toAccountId", request.getToAccountId());
            response.put("amount", request.getAmount());
            response.put("status", "COMPLETED");
            response.put("timestamp", LocalDateTime.now());
            response.put("initiatedBy", auth.getName());
            
            return ResponseEntity.status(HttpStatus.OK).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * Get customer dashboard summary
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard(Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        
        Map<String, Object> response = new HashMap<>();
        response.put("username", auth.getName());
        response.put("userId", userId);
        response.put("message", "Welcome to your banking dashboard");
        response.put("timestamp", LocalDateTime.now());
        response.put("features", new String[]{
            "View balance",
            "Instant transfer",
            "Transaction history"
        });
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Extract logged-in user ID from authentication token
     * TODO: Extract from Keycloak JWT token instead of hardcoding
     */
    private Long getLoggedInUserId(Authentication auth) {
        // For now, return 1L as placeholder
        // Later: Extract from JWT token using Keycloak integration
        // Example: (Long) auth.getPrincipal().getId()
        return 1L;
    }
}
