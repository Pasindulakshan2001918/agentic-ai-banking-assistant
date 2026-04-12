package com.agentic.ai.usecase.impl;

import com.agentic.ai.usecase.CardUseCase;
import com.agentic.ai.service.AiResponseBuilder;
import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiCardActionRequest;
import com.agentic.dto.BlockCardRequest;
import com.agentic.dto.CardActionRequest;
import com.agentic.dto.CardResponse;
import com.agentic.entity.Conversation;
import com.agentic.service.CardService;
import com.agentic.service.OtpService;
import org.springframework.stereotype.Service;

@Service
public class CardUseCaseImpl implements CardUseCase {

    private final CardService cardService;
    private final OtpService otpService;
    private final AiResponseBuilder aiResponseBuilder;

    public CardUseCaseImpl(
            CardService cardService,
            OtpService otpService,
            AiResponseBuilder aiResponseBuilder) {
        this.cardService = cardService;
        this.otpService = otpService;
        this.aiResponseBuilder = aiResponseBuilder;
    }

    @Override
    public AiBaseResponse execute(Conversation conversation, AiCardActionRequest request) {
        String operationId = aiResponseBuilder.generateOperationId();
        try {
            Long userId = conversation.getUser().getId();
            String action = request.getAction() != null ? request.getAction().toUpperCase() : "BLOCK";

            // BLOCK does not require OTP — execute immediately
            if ("BLOCK".equals(action)) {
                BlockCardRequest blockReq = new BlockCardRequest();
                blockReq.setCardId(request.getCardId());
                blockReq.setReason(request.getReason() != null ? request.getReason() : "Blocked via AI assistant");
                CardResponse cardResponse = cardService.blockCard(userId, blockReq);
                return aiResponseBuilder.buildCardActionResponse(
                    operationId, "BLOCK", "SUCCESS",
                    "Your card ending " + cardResponse.getLastFourDigits() + " has been blocked.",
                    conversation
                );
            }

            // UNBLOCK and TOGGLE actions require OTP
            if (conversation.getState() != null &&
                    conversation.getState().toString().equals("AWAITING_OTP")) {
                if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
                    return aiResponseBuilder.buildOtpResponse(operationId, conversation);
                }
                boolean otpValid = otpService.verifyOtp(
                    userId, request.getOtpCode(), "CARD-" + conversation.getId());
                if (!otpValid) {
                    return aiResponseBuilder.buildFailureResponse(
                        operationId, "Invalid or expired OTP.", conversation);
                }
                CardActionRequest cardReq = new CardActionRequest();
                cardReq.setCardId(request.getCardId());
                CardResponse cardResponse;
                String successMessage;
                if ("UNBLOCK".equals(action)) {
                    cardResponse = cardService.unblockCard(userId, cardReq);
                    successMessage = "Card ending " + cardResponse.getLastFourDigits() + " unblocked.";
                } else {
                    successMessage = "Card action " + action + " completed.";
                }
                return aiResponseBuilder.buildCardActionResponse(
                    operationId, action, "SUCCESS", successMessage, conversation);
            }

            // First call for OTP-required action — generate OTP
            otpService.generateOtp(userId, "CARD-" + conversation.getId());
            return aiResponseBuilder.buildOtpResponse(operationId, conversation);

        } catch (Exception e) {
            return aiResponseBuilder.buildFailureResponse(operationId, e.getMessage(), conversation);
        }
    }
}
