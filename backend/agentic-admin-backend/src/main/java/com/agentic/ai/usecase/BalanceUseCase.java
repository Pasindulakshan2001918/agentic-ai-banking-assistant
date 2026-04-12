package com.agentic.ai.usecase;

import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiBalanceRequest;
import com.agentic.entity.Conversation;

public interface BalanceUseCase {
    AiBaseResponse execute(Conversation conversation, AiBalanceRequest request);
}
