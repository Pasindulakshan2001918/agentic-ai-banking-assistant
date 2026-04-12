package com.agentic.controller;

import com.agentic.dto.*;
import com.agentic.entity.BillType;
import com.agentic.entity.Transaction;
import com.agentic.exception.UnauthorizedException;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.TransactionRepository;
import com.agentic.repository.UserRepository;
import com.agentic.security.SecurityUtils;
import com.agentic.service.BillService;
import com.agentic.service.SpendingInsightService;
import com.agentic.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer")
public class CustomerBankingController {
    
    private final TransactionService transactionService;
    private final SpendingInsightService spendingInsightService;
    private final AccountRepository accountRepository;
    private final BillService billService;
    private final TransactionRepository transactionRepository;
    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    
    public CustomerBankingController(TransactionService transactionService,
                                     SpendingInsightService spendingInsightService,
                                     AccountRepository accountRepository,
                                     BillService billService,
                                     TransactionRepository transactionRepository,
                                     SecurityUtils securityUtils,
                                     UserRepository userRepository) {
        this.transactionService = transactionService;
        this.spendingInsightService = spendingInsightService;
        this.accountRepository = accountRepository;
        this.billService = billService;
        this.transactionRepository = transactionRepository;
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
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
            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
            
            // Check ownership
            if (!account.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("accountId", account.getId());
            response.put("accountNumber", account.getAccountNumber());
            response.put("balance", account.getBalance());
            response.put("currency", "LKR");
            response.put("status", account.getStatus());
            response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
            var account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));
            
            // Check ownership
            if (!account.getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", account.getId());
            response.put("accountNumber", account.getAccountNumber());
            response.put("accountType", account.getAccountType());
            response.put("balance", account.getBalance());
            response.put("status", account.getStatus());
            response.put("currency", "LKR");
            response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
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
        Long userId = getLoggedInUserId(auth);
        
        // 🔒 Validate user owns the FROM account
        var fromAccount = accountRepository.findById(request.getFromAccountId())
            .orElseThrow(() -> new RuntimeException("Source account not found"));
        if (!fromAccount.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not own the source account");
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
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        response.put("initiatedBy", auth.getName());
        
        return ResponseEntity.status(HttpStatus.OK).body(response);
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
        response.put("timestamp", LocalDateTime.now(ZoneOffset.UTC));
        response.put("features", new String[]{
            "View balance",
            "Instant transfer",
            "Transaction history"
        });
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Get spending insights and analysis for a given month
     * Includes category breakdown, month-over-month comparison, and budget warnings
     * CRITICAL FOR AI: AI assistant calls this to get spending summaries
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/insights/spending")
    public ResponseEntity<SpendingInsightResponse> getSpendingInsights(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            Authentication auth) {
        
        Long userId = getLoggedInUserId(auth);
        Long accountId = accountRepository.findPrimaryByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found", "Account", 0L))
            .getId();

        // Default to current month if not specified
        if (year == 0 || month == 0) {
            LocalDate now = LocalDate.now();
            year  = now.getYear();
            month = now.getMonthValue();
        }

        SpendingInsightResponse insight = spendingInsightService.getMonthlySummary(accountId, year, month);
        return ResponseEntity.ok(insight);
    }
    
    /**
     * Get a specific bill by type (ELECTRICITY, WATER, MOBILE_RECHARGE)
     * AI calls this first to show the user what they owe.
     * CRITICAL FOR AI: Fetches outstanding bill for payment flow
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/bills/{billType}")
    public ResponseEntity<BillResponse> getBill(
            @PathVariable BillType billType,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(billService.getBill(userId, billType));
    }

    /**
     * Get all pending bills for a user.
     * Shows dashboard view of outstanding bills.
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/bills")
    public ResponseEntity<List<BillResponse>> getAllPendingBills(Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(billService.getAllPendingBills(userId));
    }

    /**
     * Pay a bill after OTP verification.
     * Atomic transaction: OTP check → balance deduction → bill marked PAID → transaction recorded.
     * CRITICAL FOR AI: Final step in bill payment flow
     */
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/bills/pay")
    public ResponseEntity<BillPaymentResponse> payBill(
            @Valid @RequestBody BillPaymentRequest request,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        return ResponseEntity.ok(billService.payBill(userId, request));
    }
    
    /**
     * GET /api/customer/transactions/{accountId}
     * Retrieve recent transactions for an account with pagination
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transactions/{accountId}")
    public ResponseEntity<?> getRecentTransactions(
            @PathVariable Long accountId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        Long userId = getLoggedInUserId(auth);
        var account = accountRepository.findById(accountId)
            .orElseThrow(() -> new RuntimeException("Account not found"));
        if (!account.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You do not own this account");
        }
        Page<Transaction> transactions = transactionRepository
            .findByAccountId(accountId, PageRequest.of(page, size));
        return ResponseEntity.ok(transactions);
    }
    
    /**
     * GET /api/customer/transactions
     * Retrieve recent transaction history for user.
     * Critical for AI to answer: "What did I spend last week?"
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        String userId = securityUtils.getUserId(auth);
        // TODO: Change accountRepository to work with userId (UUID string)
        // For now, temporarily use old method
        Long numericUserId = getLoggedInUserId(auth);
        // Get primary account
        var account = accountRepository.findPrimaryByUserId(numericUserId)
            .orElseThrow(() -> new EntityNotFoundException("No primary account found for user", "Account", 0L));
        
        // Get recent transactions using pagination
        var page = transactionRepository.findByAccountId(
            account.getId(), 
            org.springframework.data.domain.PageRequest.of(0, limit)
        );
        
        return ResponseEntity.ok(
            page.getContent().stream()
                .map(TransactionResponse::fromEntity)
                .toList()
        );
    }
    
    /**
     * GET /api/customer/summary
     * Retrieve account summary with total balance.
     * Critical for AI to answer: "How much money do I have?"
     */
    @PreAuthorize("isAuthenticated()")
    @GetMapping("/summary")
    public ResponseEntity<AccountSummary> getAccountSummary(
            Authentication auth) {
        String userId = securityUtils.getUserId(auth);
        // TODO: Change accountRepository to work with userId (UUID string)
        Long numericUserId = getLoggedInUserId(auth);
        List<com.agentic.entity.Account> accounts = accountRepository.findByUserId(numericUserId);
        if (accounts.isEmpty()) {
            throw new EntityNotFoundException("No accounts found for user", "Account", 0L);
        }
        return ResponseEntity.ok(AccountSummary.fromEntities(accounts));
    }
    
    /**
     * 🔐 Extract logged-in user ID from JWT token (Keycloak)
     * Keycloak JWT contains "sub" claim with user UUID
     * @param auth Spring Security Authentication object with JWT
     * @return User ID from token
     * @throws UnauthorizedException if authentication is missing or token has no user ID
     */
    private Long getLoggedInUserId(Authentication auth) {
        if (auth == null) {
            throw new com.agentic.exception.UnauthorizedException(
                "No authentication provided");
        }

        // CUSTOMER route — principal is a User entity (set by CustomerJwtFilter)
        if (auth.getPrincipal() instanceof com.agentic.entity.User user) {
            return user.getId();
        }

        // ADMIN route — principal is a Keycloak JWT
        if (auth.getPrincipal() instanceof
                org.springframework.security.oauth2.jwt.Jwt jwt) {
            String subject = jwt.getSubject();
            try {
                return Long.parseLong(subject);
            } catch (NumberFormatException e) {
                return userRepository.findByKeycloakId(subject)
                        .map(com.agentic.entity.User::getId)
                        .orElseThrow(() ->
                            new com.agentic.exception.EntityNotFoundException(
                                "Admin user not found for Keycloak subject: "
                                + subject, "User", 0L));
            }
        }

        throw new com.agentic.exception.UnauthorizedException(
            "Unrecognised authentication type: "
            + auth.getPrincipal().getClass().getSimpleName());
    }
}
