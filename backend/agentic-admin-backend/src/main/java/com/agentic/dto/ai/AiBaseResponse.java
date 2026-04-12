
package com.agentic.dto.ai;

import java.time.LocalDateTime;

/**

* BASE RESPONSE DTO

* All AI responses inherit from this

*

* 🔒 DTO-ONLY: No entity references

* 🔒 FORMATTING: Conversation state, messages, next steps

*/

public class AiBaseResponse {

private String operationId;

private String status; // SUCCESS, PENDING, FAILED, AWAITING_CONFIRMATION

private String message;

private String conversationState; // Current state for frontend

private String nextAction; // What user should do next

private LocalDateTime timestamp;

public AiBaseResponse() {

this.timestamp = LocalDateTime.now();

}

public AiBaseResponse(String operationId, String status, String message)
{

this.operationId = operationId;

this.status = status;

this.message = message;

this.timestamp = LocalDateTime.now();

}

// Getters & Setters

public String getOperationId() {

return operationId;

}

public void setOperationId(String operationId) {

this.operationId = operationId;

}

public String getStatus() {

return status;

}

public void setStatus(String status) {

this.status = status;

}

public String getMessage() {

return message;

}

public void setMessage(String message) {

this.message = message;

}

public String getConversationState() {

return conversationState;

}

public void setConversationState(String conversationState) {

this.conversationState = conversationState;

}

public String getNextAction() {

return nextAction;

}

public void setNextAction(String nextAction) {

this.nextAction = nextAction;

}

public LocalDateTime getTimestamp() {

return timestamp;

}

public void setTimestamp(LocalDateTime timestamp) {

this.timestamp = timestamp;

}

}
