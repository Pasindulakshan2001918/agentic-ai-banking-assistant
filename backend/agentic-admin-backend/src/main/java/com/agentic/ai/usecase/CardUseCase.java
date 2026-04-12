package com.agentic.ai.usecase;

import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiCardActionRequest;
import com.agentic.entity.Conversation;

public interface CardUseCase {
    AiBaseResponse execute(Conversation conversation, AiCardActionRequest request);
}
