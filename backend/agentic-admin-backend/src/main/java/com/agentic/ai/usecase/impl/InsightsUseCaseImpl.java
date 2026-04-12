package com.agentic.ai.usecase.impl;

import com.agentic.ai.usecase.InsightsUseCase;
import com.agentic.dto.ai.AiBaseResponse;
import com.agentic.dto.ai.AiInsightsRequest;
import com.agentic.entity.Conversation;
import org.springframework.stereotype.Service;

@Service
public class InsightsUseCaseImpl implements InsightsUseCase {

    @Override
    public AiBaseResponse execute(Conversation conversation, AiInsightsRequest request) {
        AiBaseResponse response = new AiBaseResponse();
        response.setStatus("SUCCESS");
        response.setMessage("Insights generated for period: " + request.getPeriod());
        return response;
    }
}
