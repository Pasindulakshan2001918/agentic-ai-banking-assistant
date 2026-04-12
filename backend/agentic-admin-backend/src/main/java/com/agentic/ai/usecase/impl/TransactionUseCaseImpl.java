package com.agentic.ai.usecase.impl;

import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiTransactionRequest;
import com.agentic.entity.Conversation;
import org.springframework.stereotype.Service;

@Service
public class TransactionUseCaseImpl {

    public AiBaseResponse execute(Conversation conversation, AiTransactionRequest request) {
        AiBaseResponse response = new AiBaseResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Transaction history retrieved");
        return response;
    }
}
