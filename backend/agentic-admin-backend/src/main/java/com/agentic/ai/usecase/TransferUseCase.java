package com.agentic.ai.usecase;

import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiTransferRequest;
import com.agentic.entity.Conversation;

public interface TransferUseCase {
    AiBaseResponse execute(Conversation conversation, AiTransferRequest request);
}
