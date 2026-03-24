"""
Pydantic models for AI Service requests/responses
"""

from typing import Optional, List, Dict, Any
from datetime import datetime
from pydantic import BaseModel, Field
from enum import Enum


class IntentType(str, Enum):
    """Supported intents"""
    TRANSFER = "TRANSFER"
    CHECK_BALANCE = "CHECK_BALANCE"
    VIEW_TRANSACTIONS = "VIEW_TRANSACTIONS"
    PAY_BILL = "PAY_BILL"
    GET_INSIGHTS = "GET_INSIGHTS"
    BLOCK_CARD = "BLOCK_CARD"
    UNBLOCK_CARD = "UNBLOCK_CARD"
    TOGGLE_PAYMENT_METHOD = "TOGGLE_PAYMENT_METHOD"
    SCHEDULE_TRANSFER = "SCHEDULE_TRANSFER"
    UNKNOWN = "UNKNOWN"


class ChatRequest(BaseModel):
    """User message to AI service"""
    user_id: str = Field(..., description="Unique user identifier")
    message: str = Field(..., description="User's natural language input")
    auth_token: Optional[str] = Field(None, description="JWT token for backend API calls")
    session_id: Optional[str] = Field(None, description="Conversation session ID")


class ChatResponse(BaseModel):
    """AI response to user"""
    message: str = Field(..., description="AI's response text")
    intent: Optional[IntentType] = Field(None, description="Detected intent")
    confidence: float = Field(..., description="Confidence score (0-1)")
    require_confirmation: bool = Field(False, description="Whether user confirmation is needed")
    session_id: str = Field(..., description="Session ID for state tracking")
    action_data: Optional[Dict[str, Any]] = Field(None, description="Data for confirmed action")


class IntentOutput(BaseModel):
    """Result from intent detection"""
    intent: IntentType = Field(..., description="Detected intent type")
    entities: Dict[str, Any] = Field(default_factory=dict, description="Extracted entities (amount, recipient, etc.)")
    confidence: float = Field(..., description="Intent detection confidence (0-1)")
    raw_text: str = Field(..., description="Original user input")


class RequiredField(BaseModel):
    """Description of a missing required field"""
    name: str = Field(..., description="Field name (amount, recipient, etc.)")
    type: str = Field(..., description="Expected type (number, text, date, etc.)")
    description: str = Field(..., description="User-friendly description")


class DialogueResponse(BaseModel):
    """Response from dialogue manager"""
    should_execute: bool = Field(..., description="Whether ready to execute")
    missing_fields: List[RequiredField] = Field(default_factory=list, description="Missing required fields")
    question: str = Field(..., description="Question to ask user if missing fields")
    require_confirmation: bool = Field(False, description="Whether final confirmation needed")
    context_summary: Optional[str] = Field(None, description="Summary of understood context")


class Message(BaseModel):
    """Single message in conversation"""
    role: str = Field(..., description="'user' or 'ai'")
    content: str = Field(..., description="Message text")
    timestamp: datetime = Field(default_factory=datetime.utcnow)


class UserSession(BaseModel):
    """User's conversation session with state"""
    session_id: str = Field(default_factory=lambda: f"session_{datetime.utcnow().timestamp()}")
    user_id: str = Field(..., description="User ID")
    messages: List[Message] = Field(default_factory=list, description="Conversation history")
    context: Dict[str, Any] = Field(default_factory=dict, description="Conversation context (entities, extracted data)")
    current_intent: Optional[IntentType] = Field(None, description="Current active intent")
    created_at: datetime = Field(default_factory=datetime.utcnow)
    last_activity: datetime = Field(default_factory=datetime.utcnow)
    
    def add_message(self, role: str, content: str):
        """Add message to conversation history"""
        self.messages.append(Message(role=role, content=content))
        self.last_activity = datetime.utcnow()
    
    def clear_intent(self):
        """Clear current intent after completion"""
        self.current_intent = None
    
    def clear_context(self):
        """Clear context (used when cancelling)"""
        self.context = {}
    
    def dict(self, **kwargs) -> Dict[str, Any]:
        """Override dict for JSON serialization"""
        data = super().model_dump(**kwargs)
        # Convert enums to strings
        if data.get("current_intent"):
            data["current_intent"] = data["current_intent"].value if isinstance(data["current_intent"], IntentType) else data["current_intent"]
        return data


class BackendExecutionRequest(BaseModel):
    """Request to execute action on backend"""
    intent: IntentType = Field(...)
    user_id: str = Field(...)
    context: Dict[str, Any] = Field(...)
    auth_token: str = Field(...)


class BackendExecutionResponse(BaseModel):
    """Response from backend execution"""
    success: bool = Field(...)
    message: str = Field(...)
    data: Optional[Dict[str, Any]] = Field(None)


class InsightData(BaseModel):
    """AI-powered insights about user's finances"""
    current_month_spending: float = Field(...)
    last_month_spending: float = Field(...)
    percent_change: float = Field(..., description="Percentage change from last month")
    top_categories: List[Dict[str, Any]] = Field(...)
    alerts: List[str] = Field(default_factory=list, description="Warning messages if thresholds exceeded")


class RateLimitStatus(BaseModel):
    """Rate limit information"""
    requests_remaining: int = Field(...)
    reset_timestamp: datetime = Field(...)
    limit: int = Field(...)
