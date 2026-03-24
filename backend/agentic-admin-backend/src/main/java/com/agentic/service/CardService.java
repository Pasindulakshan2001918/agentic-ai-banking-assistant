package com.agentic.service;

import com.agentic.dto.BlockCardRequest;
import com.agentic.dto.CardActionRequest;
import com.agentic.dto.CardResponse;
import com.agentic.entity.Account;
import com.agentic.entity.Card;
import com.agentic.exception.InvalidTransactionException;
import com.agentic.exception.UnauthorizedException;
import com.agentic.exception.ValidationException;
import com.agentic.repository.AccountRepository;
import com.agentic.repository.CardRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * SERVICE: Card Management
 * 
 * Handles card operations:
 * - List cards
 * - Block card (OTP-free for emergency cases)
 * - Unblock card (OTP required)
 * - Toggle online payments (OTP required)
 * - Toggle contactless (OTP required)
 * 
 * SECURITY PRINCIPLE: Blocking is OTP-free (speed is critical for lost/stolen cards),
 * but re-enabling and enabling payments require OTP (careful changes).
 */
@Service
public class CardService {

    private final CardRepository cardRepository;
    private final AccountRepository accountRepository;
    private final OtpService otpService;
    private final AuditService auditService;

    public CardService(CardRepository cardRepository,
                       AccountRepository accountRepository,
                       OtpService otpService,
                       AuditService auditService) {
        this.cardRepository = cardRepository;
        this.accountRepository = accountRepository;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    // ─────────────────────────────────────────────
    // READ OPERATIONS
    // ─────────────────────────────────────────────

    /**
     * List all cards linked to the user's account.
     */
    public List<CardResponse> getCards(Long userId) {
        Account account = getAccountForUser(userId);
        return cardRepository.findByAccountId(account.getId())
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────
    // BLOCK — OTP-free, immediate
    // ─────────────────────────────────────────────

    /**
     * Block a card immediately. No OTP — speed is critical here.
     * If the user lost their card, every second matters.
     * 
     * @param userId The authenticated user
     * @param request Block card request with cardId and reason
     * @return Updated card response
     */
    @Transactional
    public CardResponse blockCard(Long userId, BlockCardRequest request) {
        Card card = getCardForUser(userId, request.getCardId());

        if (card.getStatus() == Card.CardStatus.BLOCKED) {
            throw new InvalidTransactionException(
                "Card ending in " + card.getLastFourDigits() + " is already blocked."
            );
        }
        if (card.getStatus() == Card.CardStatus.EXPIRED) {
            throw new InvalidTransactionException(
                "Card ending in " + card.getLastFourDigits() + " has already expired."
            );
        }

        card.setStatus(Card.CardStatus.BLOCKED);
        card.setBlockReason(request.getReason());
        card.setBlockedAt(LocalDateTime.now());
        // Also disable online payments when blocked for safety
        card.setOnlinePaymentsEnabled(false);
        cardRepository.save(card);

        auditService.logAction("CARD", card.getId(), "BLOCKED", userId.toString(),
            null, "Card blocked for security",
            "Card ***" + card.getLastFourDigits() + " blocked. Reason: " + request.getReason());

        return toResponse(card);
    }

    // ─────────────────────────────────────────────
    // UNBLOCK — OTP required
    // ─────────────────────────────────────────────

    /**
     * Re-enable a blocked card. OTP required — this is the sensitive direction.
     * 
     * @param userId The authenticated user
     * @param request Card action request with cardId and OTP
     * @return Updated card response
     */
    @Transactional
    public CardResponse unblockCard(Long userId, CardActionRequest request) {
        verifyOtp(userId, request.getOtp());

        Card card = getCardForUser(userId, request.getCardId());

        if (card.getStatus() != Card.CardStatus.BLOCKED) {
            throw new InvalidTransactionException(
                "Card ending in " + card.getLastFourDigits() + " is not currently blocked."
            );
        }

        card.setStatus(Card.CardStatus.ACTIVE);
        card.setBlockReason(null);
        card.setBlockedAt(null);
        cardRepository.save(card);

        auditService.logAction("CARD", card.getId(), "UNBLOCKED", userId.toString(),
            null, "Card unblocked",
            "Card ***" + card.getLastFourDigits() + " unblocked.");

        return toResponse(card);
    }

    // ─────────────────────────────────────────────
    // TOGGLE ONLINE PAYMENTS — OTP required
    // ─────────────────────────────────────────────

    /**
     * Enable or disable online/e-commerce payments on the card.
     * OTP required — this changes the card's spending capability.
     * 
     * @param userId The authenticated user
     * @param request Card action request with cardId and OTP
     * @return Updated card response
     */
    @Transactional
    public CardResponse toggleOnlinePayments(Long userId, CardActionRequest request) {
        verifyOtp(userId, request.getOtp());

        Card card = getCardForUser(userId, request.getCardId());

        if (card.getStatus() == Card.CardStatus.BLOCKED) {
            throw new InvalidTransactionException(
                "Cannot change settings on a blocked card. Unblock the card first."
            );
        }

        boolean newState = !card.isOnlinePaymentsEnabled();
        card.setOnlinePaymentsEnabled(newState);
        cardRepository.save(card);

        String action = newState ? "ONLINE_PAYMENTS_ENABLED" : "ONLINE_PAYMENTS_DISABLED";
        auditService.logAction("CARD", card.getId(), action, userId.toString(),
            null, "Online payments setting changed",
            "Card ***" + card.getLastFourDigits()
            + " online payments " + (newState ? "enabled" : "disabled"));

        return toResponse(card);
    }

    // ─────────────────────────────────────────────
    // TOGGLE CONTACTLESS — OTP required
    // ─────────────────────────────────────────────

    /**
     * Enable or disable contactless payments on the card.
     * OTP required — this changes the card's spending capability.
     * 
     * @param userId The authenticated user
     * @param request Card action request with cardId and OTP
     * @return Updated card response
     */
    @Transactional
    public CardResponse toggleContactless(Long userId, CardActionRequest request) {
        verifyOtp(userId, request.getOtp());

        Card card = getCardForUser(userId, request.getCardId());

        if (card.getStatus() == Card.CardStatus.BLOCKED) {
            throw new InvalidTransactionException(
                "Cannot change settings on a blocked card."
            );
        }

        boolean newState = !card.isContactlessEnabled();
        card.setContactlessEnabled(newState);
        cardRepository.save(card);

        auditService.logAction("CARD", card.getId(), "CONTACTLESS_TOGGLED", userId.toString(),
            null, "Contactless setting changed",
            "Card ***" + card.getLastFourDigits()
            + " contactless " + (newState ? "enabled" : "disabled"));

        return toResponse(card);
    }

    // ─────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────

    /**
     * Verify OTP code
     */
    private void verifyOtp(Long userId, String otp) {
        try {
            boolean valid = otpService.verifyOtp(userId, otp, "CARD-OPERATION");
            if (!valid) {
                throw new ValidationException("Invalid or expired OTP.");
            }
        } catch (Exception e) {
            throw new ValidationException("Invalid or expired OTP.");
        }
    }

    /**
     * Get account for authenticated user
     */
    private Account getAccountForUser(Long userId) {
        return accountRepository.findPrimaryByUserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Account not found."));
    }

    /**
     * Get card and verify ownership — prevents access to other users' cards
     */
    private Card getCardForUser(Long userId, Long cardId) {
        Account account = getAccountForUser(userId);
        return cardRepository.findByIdAndAccountId(cardId, account.getId())
            .orElseThrow(() -> new UnauthorizedException(
                "Card not found or does not belong to your account."
            ));
    }

    /**
     * Convert Card entity to CardResponse DTO
     */
    private CardResponse toResponse(Card card) {
        CardResponse r = new CardResponse();
        r.setCardId(card.getId());
        r.setMaskedCardNumber(card.getMaskedCardNumber());
        r.setLastFourDigits(card.getLastFourDigits());
        r.setCardType(card.getCardType().name());
        r.setStatus(card.getStatus().name());
        r.setOnlinePaymentsEnabled(card.isOnlinePaymentsEnabled());
        r.setContactlessEnabled(card.isContactlessEnabled());
        r.setExpiryDate(card.getExpiryDate().format(
            DateTimeFormatter.ofPattern("MM/yy")));
        r.setBlockReason(card.getBlockReason());
        return r;
    }
}
