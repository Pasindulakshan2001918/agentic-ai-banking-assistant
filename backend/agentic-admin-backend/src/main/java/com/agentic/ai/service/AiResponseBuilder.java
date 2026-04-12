package com.agentic.ai.service;

import com.agentic.dto.ai.*;
import com.agentic.entity.Conversation;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AiResponseBuilder {

    public String generateOperationId() {
        return "OP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    public AiBaseResponse buildPendingResponse(String operationId, String message, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus("PENDING");
        r.setMessage(message);
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Please confirm this action");
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildSuccessResponse(String operationId, String message, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus("SUCCESS");
        r.setMessage(message);
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Transaction completed successfully");
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildFailureResponse(String operationId, String errorMessage, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus("FAILED");
        r.setMessage(errorMessage);
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Please try again or contact support.");
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildOtpResponse(String operationId, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus("AWAITING_OTP");
        r.setMessage("OTP sent to your registered phone number");
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Please enter the 6-digit OTP");
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildConfirmationResponse(String operationId, String summary, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus("AWAITING_CONFIRMATION");
        r.setMessage("Please confirm: " + summary);
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Type 'yes' to confirm or 'no' to cancel");
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildTransferResponse(String operationId, String status, String message,
                                               String recipientName, BigDecimal amount, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus(status);
        r.setMessage(message + " to " + (recipientName != null ? recipientName : "recipient") +
                    " | Amount: " + formatCurrency(amount));
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Transfer " + status.toLowerCase());
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildPaymentResponse(String operationId, String status, String message,
                                              String billProvider, BigDecimal amount, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus(status);
        r.setMessage(message + " | " + billProvider + " | Amount: " + formatCurrency(amount));
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Payment " + status.toLowerCase());
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public AiBaseResponse buildCardActionResponse(String operationId, String action, String status,
                                                 String message, Conversation conv) {
        AiBaseResponse r = new AiBaseResponse();
        r.setOperationId(operationId);
        r.setStatus(status);
        r.setMessage(message);
        r.setConversationState(conv.getState().toString());
        r.setNextAction("Card action " + action + " " + status.toLowerCase());
        r.setTimestamp(LocalDateTime.now());
        return r;
    }

    public String formatCurrency(BigDecimal amount) {
        if (amount == null) return "LKR 0.00";
        return "LKR " + amount;
    }
}
