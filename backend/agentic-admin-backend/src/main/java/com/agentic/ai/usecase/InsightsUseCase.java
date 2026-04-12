package com.agentic.ai.usecase;

import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiInsightsRequest;
import com.agentic.entity.Conversation;

public interface InsightsUseCase {
    AiBaseResponse execute(Conversation conversation, AiInsightsRequest request);
}
