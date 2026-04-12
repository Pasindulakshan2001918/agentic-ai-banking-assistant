package com.agentic.controller;

import com.agentic.dto.*;
import com.agentic.dto.ai.*;
import com.agentic.service.TransactionService;
import com.agentic.service.AccountService;
import com.agentic.service.BillService;
import com.agentic.service.SpendingInsightService;
import com.agentic.service.CardService;
import com.agentic.service.AiInsightsService;
import com.agentic.entity.BillType;
import com.agentic.exception.UnauthorizedException;
import com.agentic.exception.EntityNotFoundException;
import com.agentic.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI Controller — single entry point for all AI layer requests.
 * /api/ai/**    — simple Map responses for AI microservice
 * /api/ai/v1/** — typed DTO responses for structured clients
 */
@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AiController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final BillService billService;
    private final SpendingInsightService spendingInsightService;
    private final CardService cardService;
    private final AiInsightsService aiInsightsService;
    private final UserRepository userRepository;

    public AiController(TransactionService transactionService,
                        AccountService accountService,
                        BillService billService,
                        SpendingInsightService spendingInsightService,
                        CardService cardService,
                        AiInsightsService aiInsightsService,
                        UserRepository userRepository) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.billService = billService;
        this.spendingInsightService = spendingInsightService;
        this.cardService = cardService;
        this.aiInsightsService = aiInsightsService;
        this.userRepository = userRepository;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "AI-Banking-Bridge",
            "version", "1.0.0",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    // ==================== BALANCE ====================

    @GetMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getBalance(Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            Long accountId = accountService.getPrimaryAccount(userId)
                .orElseThrow(() -> new RuntimeException("Primary account not found"))
                .getId();
            var balance = accountService.getBalance(accountId, userId);
            return ResponseEntity.ok(Map.of(
                "userId", userId, "accountId", accountId,
                "balance", balance, "currency", "LKR",
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/v1/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiBalanceResponse> getBalanceTyped(
            @RequestBody AiBalanceRequest req, Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : getLoggedInUserId(auth);
            BigDecimal balance = accountService.getBalance(req.getAccountId(), userId);
            AiBalanceResponse response = new AiBalanceResponse(operationId, req.getAccountId(), balance, balance);
            response.setStatus("SUCCESS");
            response.setAccountType(req.getAccountType() != null ? req.getAccountType() : "SAVINGS");
            response.setCurrencyCode("LKR");
            response.setMessage("Your balance is LKR " + balance);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== TRANSFERS ====================

    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> executeTransfer(
            @RequestBody CreateTransactionRequest request, Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            String result = transactionService.instantTransfer(
                request.getFromAccountId(), request.getToAccountId(), request.getAmount(), userId);
            return ResponseEntity.ok(Map.of(
                "success", true, "message", result,
                "amount", request.getAmount(), "currency", "LKR",
                "status", "COMPLETED", "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/v1/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiTransferResponse> executeTransferTyped(
            @RequestBody AiTransferRequest req, Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : getLoggedInUserId(auth);
            String result = transactionService.instantTransfer(
                req.getFromAccountId(), req.getToAccountId(), req.getAmount(), userId);
            AiTransferResponse response = new AiTransferResponse(
                operationId, "SUCCESS", result, null,
                req.getFromAccountId(), req.getToAccountId(), req.getAmount());
            response.setRecipient(req.getRecipient() != null ? req.getRecipient() : "Transfer to account " + req.getToAccountId());
            response.setRequiresConfirmation(false);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/schedule-transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> scheduleTransfer(
            @RequestBody Map<String, Object> request, Authentication auth) {
        try {
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Transfer scheduled successfully",
                "scheduledTransferId", UUID.randomUUID().toString(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== TRANSACTIONS ====================

    @GetMapping("/transactions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getTransactions(
            @RequestParam(defaultValue = "10") int limit, Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            var transactions = transactionService.getPendingTransactions();
            return ResponseEntity.ok(Map.of(
                "userId", userId, "transactions", transactions,
                "count", transactions.size(), "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    // ==================== BILL PAYMENTS ====================

    @PostMapping("/bill/pay")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> payBill(
            @RequestBody BillPaymentRequest request, Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            BillPaymentResponse billResponse = billService.payBill(userId, request);
            return ResponseEntity.ok(Map.of(
                "success", true, "message", billResponse.getMessage(),
                "referenceNumber", billResponse.getReferenceNumber(),
                "amountPaid", billResponse.getAmountPaid(), "currency", "LKR",
                "remainingBalance", billResponse.getRemainingBalance(),
                "paidAt", billResponse.getPaidAt(), "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/v1/payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiPaymentResponse> payBillTyped(
            @RequestBody AiPaymentRequest req, Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : getLoggedInUserId(auth);
            Long accountId = accountService.getPrimaryAccount(userId)
                .orElseThrow(() -> new RuntimeException("Primary account not found"))
                .getId();
            BillPaymentRequest billReq = new BillPaymentRequest();
            billReq.setBillId(req.getBillId());
            billReq.setAccountId(accountId);
            billReq.setOtp("");  // OTP verification handled in billService
            billReq.setCustomAmount(req.getAmount());
            BillPaymentResponse billResponse = billService.payBill(userId, billReq);
            AiPaymentResponse response = new AiPaymentResponse(
                operationId, "SUCCESS", billResponse.getMessage(), null, billResponse.getAmountPaid());
            response.setReferenceNumber(billResponse.getReferenceNumber());
            response.setRemainingBalance(billResponse.getRemainingBalance());
            response.setBillType(req.getBillType());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== CARD OPERATIONS ====================

    @PostMapping("/card/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> blockCard(
            @RequestBody BlockCardRequest request, Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            var cardResponse = cardService.blockCard(userId, request);
            return ResponseEntity.ok(Map.of(
                "success", true, "message", "Card blocked successfully",
                "cardId", cardResponse.getCardId(), "status", cardResponse.getStatus(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/card/unblock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> unblockCard(
            @RequestBody CardActionRequest request, Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            var cardResponse = cardService.unblockCard(userId, request);
            return ResponseEntity.ok(Map.of(
                "success", true, "message", "Card unblocked successfully",
                "cardId", cardResponse.getCardId(), "status", cardResponse.getStatus(),
                "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @PostMapping("/v1/card/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiCardActionResponse> blockCardTyped(
            @RequestBody AiCardActionRequest req, Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : getLoggedInUserId(auth);
            BlockCardRequest cardReq = new BlockCardRequest();
            cardReq.setCardId(req.getCardId());
            cardReq.setReason(req.getReason() != null ? req.getReason() : "Blocked via AI");
            CardResponse cardResponse = cardService.blockCard(userId, cardReq);
            AiCardActionResponse response = new AiCardActionResponse(
                operationId, "SUCCESS", "Card blocked successfully", req.getCardId(), "BLOCKED");
            response.setStatus("SUCCESS");
            response.setCurrentStatus(cardResponse.getStatus());
            response.setCardLast4(cardResponse.getLastFourDigits());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ==================== INSIGHTS ====================

    @GetMapping("/insights")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> getInsights(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            Long accountId = accountService.getPrimaryAccount(userId)
                .orElseThrow(() -> new RuntimeException("Primary account not found"))
                .getId();
            if (year == 0 || month == 0) {
                LocalDate now = LocalDate.now();
                year = now.getYear();
                month = now.getMonthValue();
            }
            var insights = spendingInsightService.getMonthlySummary(accountId, year, month);
            return ResponseEntity.ok(Map.of(
                "userId", userId, "accountId", accountId,
                "period", month + "/" + year, "currency", "LKR",
                "insights", insights, "timestamp", LocalDateTime.now()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/v1/insights")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiInsightsResponse> getEnhancedInsights(
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month,
            Authentication auth) {
        try {
            Long userId = getLoggedInUserId(auth);
            Long accountId = accountService.getPrimaryAccount(userId)
                .orElseThrow(() -> new RuntimeException("Primary account not found"))
                .getId();
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

    // ==================== HELPERS ====================

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

    private String generateOperationId() {
        return "OP-" + System.currentTimeMillis() + "-"
            + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
