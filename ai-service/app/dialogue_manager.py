"""
Dialogue Manager
================================================================================
State machine for managing conversational flow
Asks for missing information before executing actions
Handles confirmations, edge cases, and ambiguous inputs
================================================================================
"""

from typing import Dict, List, Any, Optional
from app.models import (
    IntentType,
    DialogueResponse,
    RequiredField,
    Message
)


class DialogueManager:
    """Manages conversation state and dialogue flow"""
    
    def __init__(self):
        self.intent_requirements = {
            IntentType.TRANSFER: {
                "required": ["recipient", "amount"],
                "optional": [],
                "requires_confirmation": True,
                "validation": self._validate_transfer
            },
            IntentType.CHECK_BALANCE: {
                "required": [],
                "optional": [],
                "requires_confirmation": False,
                "validation": None
            },
            IntentType.VIEW_TRANSACTIONS: {
                "required": [],
                "optional": ["period", "category"],
                "requires_confirmation": False,
                "validation": None
            },
            IntentType.PAY_BILL: {
                "required": ["bill_type"],
                "optional": [],
                "requires_confirmation": True,
                "validation": self._validate_bill_payment
            },
            IntentType.GET_INSIGHTS: {
                "required": [],
                "optional": [],
                "requires_confirmation": False,
                "validation": None
            },
            IntentType.BLOCK_CARD: {
                "required": [],
                "optional": [],
                "requires_confirmation": True,
                "validation": self._validate_card_action
            },
            IntentType.SCHEDULE_TRANSFER: {
                "required": ["recipient", "amount", "date"],
                "optional": ["time"],
                "requires_confirmation": True,
                "validation": self._validate_scheduled_transfer
            }
        }
        
        self.field_descriptions = {
            "recipient": {
                "type": "text",
                "description": "Who should receive the money?",
                "examples": "e.g., Nimal, friend, merchant"
            },
            "amount": {
                "type": "number",
                "description": "How much?",
                "examples": "e.g., 5000, 50k"
            },
            "bill_type": {
                "type": "choice",
                "description": "Which bill?",
                "options": ["ELECTRICITY", "WATER", "MOBILE_RECHARGE"]
            },
            "date": {
                "type": "date",
                "description": "When?",
                "examples": "tomorrow, next Friday, 2024-03-25"
            },
            "period": {
                "type": "choice",
                "description": "Which period?",
                "options": ["last_month", "this_month", "last_week", "last_3_months"]
            },
            "category": {
                "type": "choice",
                "description": "Which category?",
                "options": ["FOOD", "TRANSPORT", "UTILITIES", "SHOPPING", "ENTERTAINMENT"]
            }
        }
    
    async def manage(
        self,
        intent: IntentType,
        context: Dict[str, Any],
        conversation_history: List[Message]
    ) -> DialogueResponse:
        """
        Manage dialogue flow
        
        Returns:
            DialogueResponse indicating if ready to execute or if missing fields
        """
        
        if intent == IntentType.UNKNOWN:
            return DialogueResponse(
                should_execute=False,
                missing_fields=[],
                question="I'm not sure what you'd like to do. Could you rephrase that? "
                         "(e.g., 'Send 5000 to Nimal' or 'Check my balance')",
                require_confirmation=False
            )
        
        requirements = self.intent_requirements.get(intent)
        if not requirements:
            # Fallback for unmapped intents
            requirements = {
                "required": [],
                "optional": [],
                "requires_confirmation": False,
                "validation": None
            }
        
        # Check for missing required fields
        missing = []
        for field in requirements["required"]:
            if field not in context or context[field] is None:
                missing.append(RequiredField(
                    name=field,
                    type=self.field_descriptions[field].get("type", "text"),
                    description=self.field_descriptions[field].get("description", "")
                ))
        
        # If missing required fields, ask user
        if missing:
            questions = [f.description for f in missing]
            question_text = " ".join(questions)
            
            return DialogueResponse(
                should_execute=False,
                missing_fields=missing,
                question=question_text,
                require_confirmation=False,
                context_summary=self._summarize_context(intent, context)
            )
        
        # Validate extracted values
        if requirements["validation"]:
            validation_error = requirements["validation"](context)
            if validation_error:
                return DialogueResponse(
                    should_execute=False,
                    missing_fields=[],
                    question=f"❌ {validation_error}. Please try again.",
                    require_confirmation=False
                )
        
        # All required fields present - ready to execute
        return DialogueResponse(
            should_execute=True,
            missing_fields=[],
            question=self._confirm_message(intent, context),
            require_confirmation=requirements["requires_confirmation"],
            context_summary=self._summarize_context(intent, context)
        )
    
    def _summarize_context(self, intent: IntentType, context: Dict[str, Any]) -> str:
        """Generate summary of understood context"""
        summaries = {
            IntentType.TRANSFER: f"Transfer ₹{context.get('amount', '?')} to {context.get('recipient', 'unknown')}",
            IntentType.PAY_BILL: f"Pay {context.get('bill_type', 'unknown')} bill",
            IntentType.SCHEDULE_TRANSFER: f"Schedule ₹{context.get('amount', '?')} to {context.get('recipient', 'unknown')} on {context.get('date', 'unknown')}",
            IntentType.VIEW_TRANSACTIONS: f"Show transactions from {context.get('period', 'all time')}",
            IntentType.BLOCK_CARD: "Block card for security"
        }
        return summaries.get(intent, intent.value)
    
    def _confirm_message(self, intent: IntentType, context: Dict[str, Any]) -> str:
        """Generate confirmation message"""
        messages = {
            IntentType.TRANSFER: f"Transfer ₹{context.get('amount')} to {context.get('recipient')}?",
            IntentType.PAY_BILL: f"Pay your {context.get('bill_type', 'utilities')} bill?",
            IntentType.SCHEDULE_TRANSFER: f"Schedule ₹{context.get('amount')} transfer to {context.get('recipient')} for {context.get('date')}?",
            IntentType.BLOCK_CARD: "Block your card immediately?",
            IntentType.UNBLOCK_CARD: "Unblock your card?"
        }
        return messages.get(intent, "Confirm this action?")
    
    # ========== VALIDATION METHODS ==========
    
    def _validate_transfer(self, context: Dict[str, Any]) -> Optional[str]:
        """Validate transfer fields"""
        amount = context.get("amount")
        if amount and amount <= 0:
            return "Amount must be positive"
        
        recipient = context.get("recipient", "").strip()
        if not recipient:
            return "Recipient is required"
        
        # Check for daily limit (500,000 LKR = 250,000 USD equivalent simulation)
        if amount and amount > 5000000:
            return "Transfer exceeds daily limit (₹5,000,000)"
        
        return None
    
    def _validate_bill_payment(self, context: Dict[str, Any]) -> Optional[str]:
        """Validate bill payment fields"""
        bill_type = context.get("bill_type")
        valid_types = ["ELECTRICITY", "WATER", "MOBILE_RECHARGE"]
        
        if not bill_type or bill_type not in valid_types:
            return f"Invalid bill type. Choose from: {', '.join(valid_types)}"
        
        return None
    
    def _validate_card_action(self, context: Dict[str, Any]) -> Optional[str]:
        """Validate card action"""
        # Could check if card is already blocked, etc.
        return None
    
    def _validate_scheduled_transfer(self, context: Dict[str, Any]) -> Optional[str]:
        """Validate scheduled transfer"""
        date = context.get("date")
        if not date:
            return "Schedule date is required"
        
        # Basic validation - date should be in future
        # In production, use proper datetime parsing
        if date == "today" or date == "now":
            return "Cannot schedule for past. Use 'tomorrow' or a future date."
        
        validation_error = self._validate_transfer(context)
        if validation_error:
            return validation_error
        
        return None
    
    # ========== EDGE CASE HANDLING ==========
    
    async def handle_ambiguous_input(
        self,
        intent: IntentType,
        ambiguous_field: str,
        conversation_history: List[Message]
    ) -> str:
        """Handle ambiguous user input (e.g., "Send to John" - which John?)"""
        
        messages_by_field = {
            "recipient": "I found multiple recipients with that name. Could you be more specific? (phone number or account number?)",
            "date": "Which date did you mean?",
            "amount": "Could you clarify the amount?"
        }
        
        return messages_by_field.get(ambiguous_field, f"Could you clarify '{ambiguous_field}'?")
    
    def handle_user_cancel(self, context: Dict[str, Any]) -> str:
        """Handle user cancellation"""
        return "❌ Action cancelled. How else can I help?"
    
    def handle_mid_flow_intent_change(
        self,
        original_intent: IntentType,
        new_intent: IntentType
    ) -> str:
        """Handle when user changes intent mid-conversation"""
        return f"Understood. Switching from {original_intent.value} to {new_intent.value}. What would you like to do?"
