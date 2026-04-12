package com.agentic.ai.usecase.impl;

import com.agentic.ai.usecase.TransferUseCase;
import com.agentic.ai.service.AiResponseBuilder;
import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiTransferRequest;
import com.agentic.entity.Conversation;
import com.agentic.service.TransactionService;
import com.agentic.service.OtpService;
import org.springframework.stereotype.Service;

@Service
public class TransferUseCaseImpl implements TransferUseCase {

    private final TransactionService transactionService;
    private final OtpService otpService;
    private final AiResponseBuilder aiResponseBuilder;

    public TransferUseCaseImpl(
            TransactionService transactionService,
            OtpService otpService,
            AiResponseBuilder aiResponseBuilder) {
        this.transactionService = transactionService;
        this.otpService = otpService;
        this.aiResponseBuilder = aiResponseBuilder;
    }

    @Override
    public AiBaseResponse execute(Conversation conversation, AiTransferRequest request) {
        String operationId = aiResponseBuilder.generateOperationId();
        try {
            Long userId = conversation.getUser().getId();

            // Step 1: If conversation is waiting for OTP, verify and execute
            if (conversation.getState() != null &&
                    conversation.getState().toString().equals("AWAITING_OTP")) {
                if (request.getOtpCode() == null || request.getOtpCode().isBlank()) {
                    return aiResponseBuilder.buildOtpResponse(operationId, conversation);
                }
                // Verify OTP and complete the transfer
                boolean otpValid = otpService.verifyOtp(
                    userId,
                    request.getOtpCode(),
                    "TRANSFER-" + conversation.getId()
                );
                if (!otpValid) {
                    return aiResponseBuilder.buildFailureResponse(
                        operationId,
                        "Invalid or expired OTP. Please try again.",
                        conversation
                    );
                }
                // Execute actual transfer
                String result = transactionService.instantTransfer(
                    request.getFromAccountId(),
                    request.getToAccountId(),
                    request.getAmount(),
                    userId
                );
                return aiResponseBuilder.buildTransferResponse(
                    operationId, "SUCCESS", result,
                    request.getRecipient(), request.getAmount(), conversation
                );
            }

            // Step 2: First call — show confirmation
            if (request.getConfirmed() == null || !request.getConfirmed()) {
                String summary = "Transfer LKR " + request.getAmount() +
                    " to " + (request.getRecipient() != null ? request.getRecipient() :
                    "account " + request.getToAccountId());
                return aiResponseBuilder.buildConfirmationResponse(operationId, summary, conversation);
            }

            // Step 3: Confirmed — generate OTP
            otpService.generateOtp(userId, "TRANSFER-" + conversation.getId());
            return aiResponseBuilder.buildOtpResponse(operationId, conversation);

        } catch (Exception e) {
            return aiResponseBuilder.buildFailureResponse(operationId, e.getMessage(), conversation);
        }
    }
}
