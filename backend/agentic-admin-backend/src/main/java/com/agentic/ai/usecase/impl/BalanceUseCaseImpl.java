package com.agentic.ai.usecase.impl;

import com.agentic.ai.service.AiResponseBuilder;
import com.agentic.ai.usecase.BalanceUseCase;
import com.agentic.dto.ai.AiBalanceRequest;
import com.agentic.dto.ai.AiBalanceResponse;
import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.entity.Conversation;
import com.agentic.service.AccountService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Balance Use Case Implementation
 * Handles balance inquiry requests from AI conversations
 */
@Service
public class BalanceUseCaseImpl implements BalanceUseCase {

    private final AccountService accountService;
    private final AiResponseBuilder aiResponseBuilder;

    public BalanceUseCaseImpl(AccountService accountService, AiResponseBuilder aiResponseBuilder) {
        this.accountService = accountService;
        this.aiResponseBuilder = aiResponseBuilder;
    }

    @Override
    public AiBaseResponse execute(Conversation conversation, AiBalanceRequest request) {
        try {
            // Get user ID from conversation
            Long userId = conversation.getUser().getId();

            // Determine account ID
            Long accountId = request.getAccountId();
            if (accountId == null) {
                accountId = accountService.getPrimaryAccount(userId)
                    .orElseThrow(() -> new RuntimeException("Primary account not found"))
                    .getId();
            }

            // Get balance
            BigDecimal balance = accountService.getBalance(accountId, userId);

            // Generate operation ID
            String operationId = aiResponseBuilder.generateOperationId();

            // Build and return success response
            AiBalanceResponse response = new AiBalanceResponse(operationId, accountId, balance, balance);
            response.setStatus("SUCCESS");
            response.setMessage("Your balance is LKR " + balance);

            return (AiBaseResponse) (Object) response;
        } catch (Exception e) {
            // Build and return failure response
            return aiResponseBuilder.buildFailureResponse(
                aiResponseBuilder.generateOperationId(),
                "Could not retrieve balance: " + e.getMessage(),
                conversation
            );
        }
    }
}
