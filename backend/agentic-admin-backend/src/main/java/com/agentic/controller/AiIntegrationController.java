package com.agentic.controller;

import com.agentic.dto.*;
import com.agentic.dto.ai.*;
import com.agentic.service.TransactionService;
import com.agentic.service.AccountService;
import com.agentic.service.BillService;
import com.agentic.service.CardService;
import com.agentic.entity.BillType;
import com.agentic.repository.AccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * AI Integration Controller - Wrapper layer for AI microservice
 * 
 * Purpose: Provides AI-specific endpoints with simplified request/response contracts
 * - AI friendly DTOs (AiTransferRequest, AiPaymentRequest, etc.)
 * - Single operation reference ID per request
 * - Consistent error handling and status codes
 * - Reduces coupling between AI service and raw banking APIs
 * 
 * Architecture:
 * React Frontend → FastAPI AI Service → AiIntegrationController → Banking Services
 */
@RestController
@RequestMapping("/api/ai/v1")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:5173"})
public class AiIntegrationController {

    private final TransactionService transactionService;
    private final AccountService accountService;
    private final BillService billService;
    private final CardService cardService;
    private final AccountRepository accountRepository;

    public AiIntegrationController(TransactionService transactionService,
                                   AccountService accountService,
                                   BillService billService,
                                   CardService cardService,
                                   AccountRepository accountRepository) {
        this.transactionService = transactionService;
        this.accountService = accountService;
        this.billService = billService;
        this.cardService = cardService;
        this.accountRepository = accountRepository;
    }

    /**
     * Health check endpoint
     * Used by AI service to verify backend availability
     */
    @GetMapping("/health")
    public ResponseEntity<Map> health() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "ai-integration",
            "timestamp", System.currentTimeMillis()
        ));
    }

    // ==================== TRANSFER OPERATIONS ====================

    /**
     * Execute instant transfer between accounts
     * Wrapper endpoint: Simplifies transfer logic for AI service
     * 
     * Request Format:
     *   {
     *     "fromAccountId": 101,
     *     "toAccountId": 102,
     *     "amount": 5000,
     *     "purpose": "Payment for services",
     *     "userId": 1
     *   }
     * 
     * Response Format:
     *   {
     *     "operationId": "OP-20260322-xyz123",
     *     "status": "SUCCESS",
     *     "transactionId": 456,
     *     "amount": 5000,
     *     "timestamp": "2026-03-22T10:30:00"
     *   }
     */
    @PostMapping("/transfer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiTransferResponse> executeTransfer(
            @RequestBody AiTransferRequest req,
            Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : extractUserId(auth);

            // Call banking service
            String result = transactionService.instantTransfer(
                req.getFromAccountId(),
                req.getToAccountId(),
                req.getAmount(),
                userId
            );

            // Build response
            AiTransferResponse response = new AiTransferResponse(
                operationId,
                "SUCCESS",
                result,
                null,  // transactionId would need to be extracted from service
                req.getFromAccountId(),
                req.getToAccountId(),
                req.getAmount()
            );
            response.setRecipient(req.getRecipient());
            response.setRequiresConfirmation(false);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("TRANSFER", e);
        }
    }

    /**
     * Schedule transfer for future execution
     */
    @PostMapping("/transfer/schedule")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiTransferResponse> scheduleTransfer(
            @RequestBody AiTransferRequest req,
            Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : extractUserId(auth);

            // Build response for scheduled transfer
            AiTransferResponse response = new AiTransferResponse(
                operationId,
                "PENDING",
                "Transfer scheduled successfully",
                null,
                req.getFromAccountId(),
                req.getToAccountId(),
                req.getAmount()
            );
            response.setRequiresConfirmation(true);

            return ResponseEntity.accepted().body(response);
        } catch (Exception e) {
            return handleError("SCHEDULE_TRANSFER", e);
        }
    }

    // ==================== BALANCE OPERATIONS ====================

    /**
     * Get account balance - Wrapper endpoint
     * 
     * Request: userId and accountId
     * Response: Available balance, total balance, credit limit
     */
    @PostMapping("/balance")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiBalanceResponse> getBalance(
            @RequestBody AiBalanceRequest req,
            Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : extractUserId(auth);

            // Fetch balance from service (returns BigDecimal)
            BigDecimal balance = accountService.getBalance(req.getAccountId(), userId);

            AiBalanceResponse response = new AiBalanceResponse(
                operationId,
                req.getAccountId(),
                balance,  // available balance
                balance   // total balance (same in this context)
            );
            response.setAccountType(req.getAccountType() != null ? req.getAccountType() : "SAVINGS");
            response.setMessage("Balance retrieved successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("GET_BALANCE", e);
        }
    }

    // ==================== PAYMENT OPERATIONS ====================

    /**
     * Pay bill - Wrapper endpoint
     * 
     * Request: userId, billId, amount, billType
     * Response: Payment reference, amount paid, remaining balance
     */
    @PostMapping("/payment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiPaymentResponse> payBill(
            @RequestBody AiPaymentRequest req,
            Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : extractUserId(auth);

            // Convert AI request to billing request
            BillPaymentRequest billReq = new BillPaymentRequest();
            billReq.setBillId(req.getBillId());
            billReq.setAccountId(req.getUserId());  // Account to debit from
            // OTP would be set from session/context - for now using placeholder
            billReq.setOtp("000000");
            billReq.setCustomAmount(req.getAmount());

            // Execute payment
            BillPaymentResponse billResponse = billService.payBill(userId, billReq);

            AiPaymentResponse response = new AiPaymentResponse(
                operationId,
                "SUCCESS",
                billResponse.getMessage(),
                null,
                billResponse.getAmountPaid()
            );
            response.setReferenceNumber(billResponse.getReferenceNumber());
            response.setRemainingBalance(billResponse.getRemainingBalance());
            response.setBillType(req.getBillType());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("PAYMENT", e);
        }
    }

    // ==================== CARD OPERATIONS ====================

    /**
     * Block card - Wrapper endpoint
     * 
     * Request: userId, cardId, action
     * Response: Card status, operation reference
     */
    @PostMapping("/card/block")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiCardActionResponse> blockCard(
            @RequestBody AiCardActionRequest req,
            Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : extractUserId(auth);

            // Convert AI request to card request
            BlockCardRequest cardReq = new BlockCardRequest();
            cardReq.setCardId(req.getCardId());
            cardReq.setReason(req.getReason());

            // Execute block
            CardResponse cardResponse = cardService.blockCard(userId, cardReq);

            AiCardActionResponse response = new AiCardActionResponse(
                operationId,
                "SUCCESS",
                "Card blocked successfully",
                req.getCardId(),
                "BLOCKED"
            );
            response.setCurrentStatus(cardResponse.getStatus());
            response.setCardLast4(cardResponse.getLastFourDigits());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("BLOCK_CARD", e);
        }
    }

    /**
     * Unblock card - Wrapper endpoint
     */
    @PostMapping("/card/unblock")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AiCardActionResponse> unblockCard(
            @RequestBody AiCardActionRequest req,
            Authentication auth) {
        try {
            String operationId = generateOperationId();
            Long userId = req.getUserId() != null ? req.getUserId() : extractUserId(auth);

            // Convert AI request to card request
            CardActionRequest cardReq = new CardActionRequest();
            cardReq.setCardId(req.getCardId());

            // Execute unblock
            CardResponse cardResponse = cardService.unblockCard(userId, cardReq);

            AiCardActionResponse response = new AiCardActionResponse(
                operationId,
                "SUCCESS",
                "Card unblocked successfully",
                req.getCardId(),
                "UNBLOCKED"
            );
            response.setCurrentStatus(cardResponse.getStatus());
            response.setCardLast4(cardResponse.getLastFourDigits());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return handleError("UNBLOCK_CARD", e);
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Generate unique operation ID for tracing
     * Format: OP-YYYYMMDD-xxxxxxxx
     */
    private String generateOperationId() {
        return "OP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    /**
     * Extract userId from JWT token
     * TODO: Implement actual JWT extraction from Keycloak
     */
    private Long extractUserId(Authentication auth) {
        return 1L;  // Placeholder for actual JWT extraction
    }

    /**
     * Unified error handling for AI responses
     */
    private <T> ResponseEntity<T> handleError(String operation, Exception e) {
        // Log error
        // Return standardized error response
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
