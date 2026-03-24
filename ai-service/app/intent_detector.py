"""
Intent Detection Module
================================================================================
Uses OpenAI to detect user intent from natural language
Extracts entities (amount, recipient, dates, etc.)
Returns structured intent with confidence score
================================================================================
"""

import os
import json
import re
from typing import Optional, Dict, Any
from openai import AsyncOpenAI
from app.models import IntentOutput, IntentType


class IntentDetector:
    """Detects banking intents from user messages using OpenAI"""
    
    def __init__(self):
        self.client = AsyncOpenAI(api_key=os.getenv("OPENAI_API_KEY"))
        self.model = "gpt-4-turbo-preview"  # Or gpt-3.5-turbo for cost
        
        self.intent_definitions = {
            "TRANSFER": {
                "keywords": ["send", "transfer", "move", "pay"],
                "required_entities": ["recipient", "amount"],
                "examples": [
                    "Send 5000 to Nimal",
                    "Transfer 50000 rupees to my sister",
                    "Pay 2000 to merchant"
                ]
            },
            "CHECK_BALANCE": {
                "keywords": ["balance", "how much", "check balance", "account balance"],
                "required_entities": [],
                "examples": [
                    "How much do I have?",
                    "What's my balance?",
                    "Check my account balance"
                ]
            },
            "VIEW_TRANSACTIONS": {
                "keywords": ["transactions", "spending", "history", "expenses", "spent"],
                "required_entities": [],
                "optional_entities": ["period", "category"],
                "examples": [
                    "Show my transactions",
                    "What did I spend last month?",
                    "Show me food expenses"
                ]
            },
            "PAY_BILL": {
                "keywords": ["pay bill", "electricity", "water", "phone", "mobile"],
                "required_entities": ["bill_type"],
                "examples": [
                    "Pay my electricity bill",
                    "Recharge my mobile",
                    "Pay water bill"
                ]
            },
            "GET_INSIGHTS": {
                "keywords": ["insights", "analysis", "spending", "trend", "comparison"],
                "required_entities": [],
                "examples": [
                    "Am I spending more this month?",
                    "Show me my spending insights",
                    "Compare this month vs last month"
                ]
            },
            "BLOCK_CARD": {
                "keywords": ["block", "freeze", "card", "security"],
                "required_entities": [],
                "examples": [
                    "Block my card",
                    "Freeze my card immediately"
                ]
            },
            "SCHEDULE_TRANSFER": {
                "keywords": ["schedule", "later", "tomorrow", "next", "remind"],
                "required_entities": ["recipient", "amount"],
                "optional_entities": ["date", "time"],
                "examples": [
                    "Schedule 5000 to Nimal for tomorrow",
                    "Transfer 10000 to John next Friday"
                ]
            }
        }
    
    async def detect(self, text: str, context: Dict[str, Any] = None) -> IntentOutput:
        """
        Detect intent from user input
        
        Returns:
            IntentOutput with detected intent, entities, and confidence
        """
        context = context or {}
        
        # Try rule-based detection first (faster, no API call)
        rule_result = self._rule_based_detect(text)
        if rule_result and rule_result.confidence > 0.9:
            return rule_result
        
        # Fall back to OpenAI for complex inputs
        return await self._openai_detect(text, context)
    
    def _rule_based_detect(self, text: str) -> Optional[IntentOutput]:
        """Fast rule-based intent detection"""
        text_lower = text.lower()
        scores = {}
        
        for intent, definition in self.intent_definitions.items():
            match_count = sum(1 for kw in definition["keywords"] if kw in text_lower)
            if match_count > 0:
                scores[intent] = match_count / len(definition["keywords"])
        
        if not scores:
            return IntentOutput(
                intent=IntentType.UNKNOWN,
                entities={},
                confidence=0.0,
                raw_text=text
            )
        
        best_intent = max(scores, key=scores.get)
        confidence = min(scores[best_intent], 1.0)
        
        # Extract entities for high-confidence matches
        entities = {}
        if confidence > 0.6:
            entities = self._extract_entities(text, IntentType[best_intent])
        
        return IntentOutput(
            intent=IntentType[best_intent],
            entities=entities,
            confidence=confidence,
            raw_text=text
        )
    
    async def _openai_detect(self, text: str, context: Dict[str, Any]) -> IntentOutput:
        """Use OpenAI for intent detection"""
        
        intent_list = "\n".join([
            f"- {intent.value}: {defs['examples'][0]}"
            for intent, defs in self.intent_definitions.items()
        ])
        
        prompt = f"""
Detect the banking intent from this user message and extract entities.

AVAILABLE INTENTS:
{intent_list}

USER MESSAGE: "{text}"

CONTEXT: {json.dumps(context)}

Respond with ONLY valid JSON (no markdown, no extra text):
{{
  "intent": "TRANSFER|CHECK_BALANCE|VIEW_TRANSACTIONS|PAY_BILL|GET_INSIGHTS|BLOCK_CARD|SCHEDULE_TRANSFER|UNKNOWN",
  "confidence": 0.0-1.0,
  "entities": {{
    "amount": null or number,
    "recipient": null or string,
    "bill_type": null or string,
    "period": null or "last_month|this_month|last_week|etc",
    "category": null or string,
    "date": null or "YYYY-MM-DD",
    "time": null or "HH:MM"
  }},
  "reasoning": "brief explanation"
}}
"""
        
        try:
            response = await self.client.chat.completions.create(
                model=self.model,
                messages=[{"role": "user", "content": prompt}],
                temperature=0.3,
                max_tokens=500
            )
            
            # Parse response
            response_text = response.choices[0].message.content.strip()
            
            # Remove markdown code blocks if present
            response_text = response_text.replace("```json", "").replace("```", "")
            
            result = json.loads(response_text)
            
            return IntentOutput(
                intent=IntentType[result.get("intent", "UNKNOWN")],
                entities=result.get("entities", {}),
                confidence=result.get("confidence", 0.0),
                raw_text=text
            )
            
        except Exception as e:
            print(f"OpenAI error: {e}")
            return IntentOutput(
                intent=IntentType.UNKNOWN,
                entities={},
                confidence=0.0,
                raw_text=text
            )
    
    def _extract_entities(self, text: str, intent: IntentType) -> Dict[str, Any]:
        """Extract entities from text using regex patterns"""
        entities = {}
        
        # Amount extraction (handles "5000", "50k", "50 thousand", etc.)
        amount_match = re.search(r'(\d+\.?\d*)\s*(?:k|thousand|lakh|lac)?', text, re.IGNORECASE)
        if amount_match:
            amount = float(amount_match.group(1))
            if 'k' in text.lower() or 'thousand' in text.lower():
                amount *= 1000
            elif 'lakh' in text.lower() or 'lac' in text.lower():
                amount *= 100000
            entities["amount"] = amount
        
        # Recipient extraction (names after "to" or "for")
        recipient_match = re.search(r'(?:to|for)\s+([A-Za-z\s]+?)(?:\s+(?:on|at|by|with)|$)', text, re.IGNORECASE)
        if recipient_match:
            entities["recipient"] = recipient_match.group(1).strip()
        
        # Bill type extraction
        bill_types = {"electricity": "ELECTRICITY", "water": "WATER", "mobile": "MOBILE_RECHARGE"}
        for pattern, bill_type in bill_types.items():
            if pattern in text.lower():
                entities["bill_type"] = bill_type
                break
        
        # Date extraction (tomorrow, next Friday, mm/dd, etc.)
        if "tomorrow" in text.lower():
            entities["date"] = "tomorrow"
        elif "next" in text.lower():
            day_match = re.search(r'next\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday)', text, re.IGNORECASE)
            if day_match:
                entities["date"] = f"next_{day_match.group(1).lower()}"
        
        return entities
