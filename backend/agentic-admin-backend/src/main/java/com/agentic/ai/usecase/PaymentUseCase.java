package com.agentic.ai.usecase;

import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiPaymentRequest;
import com.agentic.entity.Conversation;

public interface PaymentUseCase {
    AiBaseResponse execute(Conversation conversation, AiPaymentRequest request);
}
