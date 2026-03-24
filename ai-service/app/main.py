"""
AI Service for Agentic Admin Banking Platform
================================================================================
ARCHITECTURE:
  Frontend (React) → AI Service (FastAPI) → Spring Boot Backend APIs
  
RESPONSIBILITIES:
  1. Intent Detection: Parse natural language → structured commands
  2. Dialogue Management: Maintain conversation state machine
  3. Entity Extraction: Extract users, amounts, dates from text
  4. Context Management: Remember conversation history via Redis
  5. Backend Integration: Call Spring Boot APIs through wrapper endpoints

INTENTS SUPPORTED:
  - TRANSFER: "Send 5000 to Nimal" 
  - CHECK_BALANCE: "How much do I have?"
  - VIEW_TRANSACTIONS: "Show me last month's spending"
  - PAY_BILL: "Pay my electricity bill"
  - GET_INSIGHTS: "Am I spending more this month?"
  - BLOCK_CARD: "Block my card"
================================================================================
"""

import json
from datetime import datetime
from typing import Optional
from fastapi import FastAPI, HTTPException, WebSocketException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from app.intent_detector import IntentDetector
from app.dialogue_manager import DialogueManager
from app.session_manager import SessionManager
from app.backend_integration import BackendClient
from app.models import (
    ChatRequest,
    ChatResponse,
    IntentOutput,
    UserSession
)

# Initialize FastAPI app
app = FastAPI(
    title="Agentic Admin AI Service",
    description="Natural language interface for banking operations",
    version="1.0.0"
)

# CORS configuration for frontend communication
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:5173"],  # React dev servers
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize components
intent_detector = IntentDetector()
dialogue_manager = DialogueManager()
session_manager = SessionManager()
backend_client = BackendClient()


@app.get("/health")
async def health_check():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "timestamp": datetime.utcnow().isoformat(),
        "service": "ai-service"
    }


@app.post("/api/chat", response_model=ChatResponse)
async def process_chat(request: ChatRequest):
    """
    Main chat endpoint - processes user input and returns AI response
    
    Flow:
      1. Validate input
      2. Load user session from Redis
      3. Detect intent from text
      4. Manage dialogue (ask missing info OR execute)
      5. Call backend API
      6. Save session state
      7. Return response
    """
    try:
        # Load or create session
        session = await session_manager.get_session(request.user_id)
        if not session:
            session = UserSession(user_id=request.user_id)
        
        # Detect intent from user input
        intent_result = await intent_detector.detect(
            text=request.message,
            context=session.context
        )
        
        # Update session with new intent
        session.add_message("user", request.message)
        session.current_intent = intent_result.intent
        session.context.update(intent_result.entities)
        
        # Dialogue management - ask for missing required fields
        dialogue_response = await dialogue_manager.manage(
            intent=intent_result.intent,
            context=session.context,
            conversation_history=session.messages
        )
        
        # If missing required fields, ask user
        if dialogue_response.missing_fields:
            response_text = dialogue_response.question
            session.add_message("ai", response_text)
        else:
            # All required fields present - execute action
            backend_response = await backend_client.execute(
                intent=intent_result.intent,
                context=session.context,
                user_id=request.user_id,
                auth_token=request.auth_token
            )
            
            # Format response with insights
            response_text = self._format_backend_response(
                intent_result.intent,
                backend_response
            )
            session.add_message("ai", response_text)
            session.clear_intent()  # Intent completed
        
        # Save session state
        await session_manager.save_session(session)
        
        return ChatResponse(
            message=response_text,
            intent=intent_result.intent,
            confidence=intent_result.confidence,
            require_confirmation=dialogue_response.require_confirmation,
            session_id=session.session_id
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/chat/confirm", response_model=ChatResponse)
async def confirm_action(request: ChatRequest):
    """
    Confirmation endpoint - user confirms an action
    Required when require_confirmation=True
    """
    try:
        session = await session_manager.get_session(request.user_id)
        if not session:
            raise ValueError("Session not found")
        
        # Execute the confirmed action
        backend_response = await backend_client.execute(
            intent=session.current_intent,
            context=session.context,
            user_id=request.user_id,
            auth_token=request.auth_token
        )
        
        response_text = f"✅ {session.current_intent} completed successfully.\n{backend_response.get('message', '')}"
        session.add_message("ai", response_text)
        session.clear_intent()
        
        await session_manager.save_session(session)
        
        return ChatResponse(
            message=response_text,
            intent=session.current_intent,
            confidence=1.0,
            require_confirmation=False,
            session_id=session.session_id
        )
        
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.post("/api/chat/cancel")
async def cancel_action(user_id: str):
    """Cancel current intent and clear session state"""
    try:
        session = await session_manager.get_session(user_id)
        if session:
            session.clear_intent()
            session.clear_context()
            await session_manager.save_session(session)
        
        return {"status": "cancelled", "message": "Action cancelled"}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


@app.get("/api/session/{user_id}")
async def get_session(user_id: str):
    """Get current session state (for debugging/UI state sync)"""
    try:
        session = await session_manager.get_session(user_id)
        if not session:
            return {"status": "no_active_session"}
        
        return {
            "session_id": session.session_id,
            "current_intent": session.current_intent,
            "context": session.context,
            "message_count": len(session.messages),
            "created_at": session.created_at.isoformat()
        }
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))


def _format_backend_response(self, intent: str, backend_response: dict) -> str:
    """Format backend response into user-friendly message"""
    intent_templates = {
        "TRANSFER": f"✅ Transfer of {backend_response.get('amount')} to {backend_response.get('recipient')} completed.",
        "CHECK_BALANCE": f"💰 Your balance is {backend_response.get('balance')}",
        "VIEW_TRANSACTIONS": f"📊 {backend_response.get('message')}",
        "PAY_BILL": f"✅ Bill payment successful. Receipt: {backend_response.get('reference_number')}",
        "GET_INSIGHTS": f"📈 {backend_response.get('insight_message')}",
        "BLOCK_CARD": f"🔒 Your card has been blocked for security.",
    }
    
    return intent_templates.get(intent, f"✅ {backend_response.get('message', 'Action completed')}")


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
