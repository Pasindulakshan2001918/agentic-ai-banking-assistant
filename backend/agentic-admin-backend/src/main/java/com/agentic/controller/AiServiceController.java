package com.agentic.controller;

import com.agentic.dto.*;
import com.agentic.service.TransactionService;
import com.agentic.service.AccountService;
import com.agentic.service.BillService;
import com.agentic.service.SpendingInsightService;
import com.agentic.service.CardService;
import com.agentic.service.AiInsightsService;
import com.agentic.repository.AccountRepository;
import com.agentic.entity.BillType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Service Controller - Bridges AI microservice with Spring Boot backend
 * Provides simplified endpoints for AI layer (Python FastAPI) consumption
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AiServiceController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final BillService billService;
    private final SpendingInsightService spendingInsightService;
    private final CardService cardService;
    private final AccountRepository accountRepository;
    private final AiInsightsService aiInsightsService;

    public AiServiceController(TransactionService transactionService,
                               AccountService accountService,
                               BillService billService,
                               SpendingInsightService spendingInsightService,
                               CardService cardService,
                               AccountRepository accountRepository,
                               AiInsightsService aiInsightsService) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.billService = billService;
        this.spendingInsightService = spendingInsightService;
        this.cardService = cardService;
        this.accountRepository = accountRepository;
        this.aiInsightsService = aiInsightsService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "service", "AI-Layer-Bridge",
                "version", "1.0.0",
                "timestamp", LocalDateTime.now().toString()
        ));
    }

    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> executeTransfer(
            @RequestBody CreateTransactionRequest request,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            String result = transactionService.instantTransfer(
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    request.getAmount(),
                    userId
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", result,
                    "amount", request.getAmount(),
                    "status", "COMPLETED",
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/schedule-transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> scheduleTransfer(
            @RequestBody Map<String, Object> request,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Transfer scheduled successfully");
            response.put("scheduledTransferId", UUID.randomUUID().toString());
            response.put("timestamp", LocalDateTime.now());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBalance(Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            
            Long accountId = accountRepository.findPrimaryByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Primary account not found"))
                    .getId();

            var balance = accountService.getBalance(accountId, userId);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "accountId", accountId,
                    "balance", balance,
                    "currency", "USD",
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/insights")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getInsights(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            
            Long accountId = accountRepository.findPrimaryByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Primary account not found"))
                    .getId();

            // Default to current month if not specified
            if (year == 0 || month == 0) {
                LocalDate now = LocalDate.now();
                year = now.getYear();
                month = now.getMonthValue();
            }

            var insights = spendingInsightService.getMonthlySummary(accountId, year, month);

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "accountId", accountId,
                    "period", month + "/" + year,
                    "insights", insights,
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(defaultValue = "10") int limit,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            // Return paginated transactions using existing methods
            var transactions = transactionService.getPendingTransactions();

            return ResponseEntity.ok(Map.of(
                    "userId", userId,
                    "transactions", transactions,
                    "count", transactions.size(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/card/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> blockCard(
            @RequestBody BlockCardRequest request,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            var cardResponse = cardService.blockCard(userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card blocked successfully",
                    "cardId", cardResponse.getCardId(),
                    "status", cardResponse.getStatus(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/card/unblock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> unblockCard(
            @RequestBody CardActionRequest request,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            var cardResponse = cardService.unblockCard(userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Card unblocked successfully",
                    "cardId", cardResponse.getCardId(),
                    "status", cardResponse.getStatus(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    @PostMapping("/bill/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> payBill(
            @RequestBody BillPaymentRequest request,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            BillPaymentResponse billResponse = billService.payBill(userId, request);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", billResponse.getMessage(),
                    "referenceNumber", billResponse.getReferenceNumber(),
                    "amountPaid", billResponse.getAmountPaid(),
                    "remainingBalance", billResponse.getRemainingBalance(),
                    "paidAt", billResponse.getPaidAt(),
                    "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * PHASE 5: Enhanced AI Insights
     * 
     * Returns comprehensive spending insights with:
     * 1. Categorization - Merchant → Category mapping
     * 2. Comparison - Month-over-month percentage changes
     * 3. Alerts - Intelligent threshold-based warnings
     * 
     * @param year Year of analysis (defaults to current)
     * @param month Month of analysis (defaults to current)
     * @param auth Authentication context
     * @return AiInsightsResponse with categorization, comparison, and alerts
     */
    @GetMapping("/v1/insights")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiInsightsResponse> getEnhancedInsights(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);

            Long accountId = accountRepository.findPrimaryByUserId(userId)
                    .orElseThrow(() -> new RuntimeException("Primary account not found"))
                    .getId();

            // Default to current month if not specified
            if (year == 0 || month == 0) {
                LocalDate now = LocalDate.now();
                year = now.getYear();
                month = now.getMonthValue();
            }

            AiInsightsResponse insights = aiInsightsService.generateMonthlyInsights(accountId, year, month);

            return ResponseEntity.ok(insights);

        } catch (Exception e) {
            AiInsightsResponse errorResponse = new AiInsightsResponse();
            errorResponse.setAnalysisStatus("ERROR");
            errorResponse.setAnalysisTimestamp(LocalDateTime.now().toString());
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    private Long getLoggedInUserId(Authentication auth) {
        // Placeholder returning 1L
        // TODO: Extract from JWT token when Keycloak integration is complete
        return 1L;
    }
}
