package com.agentic.controller;

import com.agentic.dto.BlockCardRequest;
import com.agentic.dto.CardActionRequest;
import com.agentic.dto.CardResponse;
import com.agentic.service.CardService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * CONTROLLER: Card Management API
 * 
 * REST endpoints for card operations:
 * - GET /api/customer/cards — List all cards
 * - POST /api/customer/cards/block — Block a card (no OTP)
 * - POST /api/customer/cards/unblock — Unblock a card (OTP required)
 * - POST /api/customer/cards/toggle-online-payments — Toggle online payments (OTP required)
 * - POST /api/customer/cards/toggle-contactless — Toggle contactless (OTP required)
 */
@RestController
@RequestMapping("/api/customer/cards")
@PreAuthorize("isAuthenticated()")
public class CardController {

    private final CardService cardService;

    public CardController(CardService cardService) {
        this.cardService = cardService;
    }

    /**
     * GET /api/customer/cards
     * List all cards linked to the authenticated user's account.
     */
    @GetMapping
    public ResponseEntity<List<CardResponse>> getCards(
            @RequestHeader("userId") Long userId) {
        return ResponseEntity.ok(cardService.getCards(userId));
    }

    /**
     * POST /api/customer/cards/block
     * Block a card immediately (no OTP — security priority for lost cards).
     * 
     * @param userId Extracted from JWT
     * @param request Contains cardId and reason
     * @return Updated card response
     */
    @PostMapping("/block")
    public ResponseEntity<CardResponse> blockCard(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody BlockCardRequest request) {
        return ResponseEntity.ok(cardService.blockCard(userId, request));
    }

    /**
     * POST /api/customer/cards/unblock
     * Unblock a card (OTP required).
     * 
     * @param userId Extracted from JWT
     * @param request Contains cardId and OTP
     * @return Updated card response
     */
    @PostMapping("/unblock")
    public ResponseEntity<CardResponse> unblockCard(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody CardActionRequest request) {
        return ResponseEntity.ok(cardService.unblockCard(userId, request));
    }

    /**
     * POST /api/customer/cards/toggle-online-payments
     * Enable or disable online/e-commerce payments (OTP required).
     * 
     * @param userId Extracted from JWT
     * @param request Contains cardId and OTP
     * @return Updated card response
     */
    @PostMapping("/toggle-online-payments")
    public ResponseEntity<CardResponse> toggleOnlinePayments(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody CardActionRequest request) {
        return ResponseEntity.ok(cardService.toggleOnlinePayments(userId, request));
    }

    /**
     * POST /api/customer/cards/toggle-contactless
     * Enable or disable contactless payments (OTP required).
     * 
     * @param userId Extracted from JWT
     * @param request Contains cardId and OTP
     * @return Updated card response
     */
    @PostMapping("/toggle-contactless")
    public ResponseEntity<CardResponse> toggleContactless(
            @RequestHeader("userId") Long userId,
            @Valid @RequestBody CardActionRequest request) {
        return ResponseEntity.ok(cardService.toggleContactless(userId, request));
    }
}
