"""
Backend Integration - AI Wrapper Layer
================================================================================
Calls Spring Boot APIs through AI-specific wrapper endpoints (/api/ai/v1/*)
Purpose:
  - Decouples AI service from raw banking APIs
  - Uses AI-optimized request/response contracts
  - Reduces coupling and simplifies AI logic
  - Each endpoint returns operation ID for tracking
  
Architecture:
  AI Service → AiIntegrationController (/api/ai/v1/*) → Banking Services
  
Why Wrapper Layer?
  ✓ AI sends AiTransferRequest → simplifies AI-side logic
  ✓ Backend converts to internal DTOs → maintains separation of concerns
  ✓ Each operation gets unique ID → enables tracing and auditing
  ✓ Consistent response formats → AI doesn't parse different DTO structures
================================================================================
"""

import os
import httpx
from typing import Dict, Any, Optional
from app.models import IntentType


class BackendClient:
    """Client for calling Spring Boot AI Integration Controller"""
    
    def __init__(self):
        self.backend_url = os.getenv("BACKEND_URL", "http://localhost:8082")
        self.ai_base_url = f"{self.backend_url}/api/ai/v1"
        self.timeout = 30.0
        self.client = httpx.AsyncClient(timeout=self.timeout)
    
    async def execute(
        self,
        intent: IntentType,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Execute action on backend based on intent"""
        
        handlers = {
            IntentType.TRANSFER: self._execute_transfer,
            IntentType.CHECK_BALANCE: self._execute_check_balance,
            IntentType.VIEW_TRANSACTIONS: self._execute_view_transactions,
            IntentType.PAY_BILL: self._execute_pay_bill,
            IntentType.GET_INSIGHTS: self._execute_get_insights,
            IntentType.BLOCK_CARD: self._execute_block_card,
            IntentType.UNBLOCK_CARD: self._execute_unblock_card,
            IntentType.SCHEDULE_TRANSFER: self._execute_schedule_transfer,
        }
        
        handler = handlers.get(intent)
        if not handler:
            return {"success": False, "message": "Intent not supported"}
        
        try:
            result = await handler(context, user_id, auth_token)
            return result
        except Exception as e:
            return {"success": False, "message": f"Error: {str(e)}"}
    
    async def _execute_transfer(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Execute instant transfer via wrapper endpoint"""
        
        headers = self._get_auth_headers(auth_token)
        
        # Use AI-optimized wrapper DTO
        payload = {
            "fromAccountId": context.get("from_account_id"),
            "toAccountId": context.get("to_account_id"),
            "amount": context.get("amount"),
            "amountFormatted": context.get("amount_formatted"),  # e.g., "5k", "1 lakh"
            "recipient": context.get("recipient"),
            "purpose": context.get("purpose", "Money transfer"),
            "userId": int(user_id)
        }
        
        try:
            response = await self.client.post(
                f"{self.ai_base_url}/transfer",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            return {
                "success": True,
                "message": f"✅ Transferred ₹{context.get('amount')} to {context.get('recipient')}",
                "amount": context.get("amount"),
                "recipient": context.get("recipient"),
                "operation_id": data.get("operationId"),
                "transaction_id": data.get("transactionId"),
                "requires_confirmation": data.get("requiresConfirmation"),
                "confirmation_code": data.get("confirmationCode"),
                "status": data.get("status"),
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Transfer failed: {str(e)}"
            }
    
    async def _execute_check_balance(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Get account balance via wrapper endpoint"""
        
        headers = self._get_auth_headers(auth_token)
        
        # Use AI-optimized wrapper DTO
        payload = {
            "userId": int(user_id),
            "accountId": context.get("account_id"),
            "accountType": context.get("account_type")
        }
        
        try:
            response = await self.client.post(
                f"{self.ai_base_url}/balance",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            return {
                "success": True,
                "message": f"💰 Your available balance is ₹{data.get('availableBalance'):,.2f}",
                "available_balance": data.get("availableBalance"),
                "total_balance": data.get("totalBalance"),
                "credit_limit": data.get("creditLimit"),
                "used_credit": data.get("usedCredit"),
                "currency": data.get("currencyCode", "INR"),
                "operation_id": data.get("operationId"),
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Could not fetch balance: {str(e)}"
            }
    
    async def _execute_view_transactions(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Get transaction history"""
        
        headers = self._get_auth_headers(auth_token)
        
        try:
            response = await self.client.get(
                f"{self.backend_url}/api/customer/transactions?limit=10",
                headers=headers
            )
            response.raise_for_status()
            
            transactions = response.json()
            
            # Format transactions for display
            transaction_lines = []
            for tx in transactions[:5]:  # Show last 5
                transaction_lines.append(
                    f"• ₹{tx.get('amount')} - {tx.get('description')} ({tx.get('createdAt')})"
                )
            
            message = "Your recent transactions:\n" + "\n".join(transaction_lines)
            
            return {
                "success": True,
                "message": message,
                "transactions": transactions,
                "data": transactions
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Could not fetch transactions: {str(e)}"
            }
    
    async def _execute_pay_bill(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Pay utility bill via wrapper endpoint"""
        
        headers = self._get_auth_headers(auth_token)
        
        # Use AI-optimized wrapper DTO
        payload = {
            "userId": int(user_id),
            "billId": context.get("bill_id"),
            "amount": context.get("amount"),
            "amountFormatted": context.get("amount_formatted"),
            "billType": context.get("bill_type"),
            "provider": context.get("provider"),
            "referenceNumber": context.get("reference_number")
        }
        
        try:
            response = await self.client.post(
                f"{self.ai_base_url}/payment",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            return {
                "success": True,
                "message": f"✅ {context.get('bill_type', 'Bill')} payment of ₹{context.get('amount')} successful",
                "reference_number": data.get("referenceNumber"),
                "amount_paid": data.get("amountPaid"),
                "remaining_balance": data.get("remainingBalance"),
                "operation_id": data.get("operationId"),
                "status": data.get("status"),
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Bill payment failed: {str(e)}"
            }
    
    async def _execute_get_insights(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Get spending insights"""
        
        headers = self._get_auth_headers(auth_token)
        
        try:
            response = await self.client.get(
                f"{self.backend_url}/api/spending-insights",
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            
            current = data.get("currentMonthSpending", 0)
            last = data.get("lastMonthSpending", 0)
            percent_change = ((current - last) / last * 100) if last > 0 else 0
            
            trend = "📈 UP" if percent_change > 0 else "📉 DOWN"
            message = f"{trend} Your spending this month is ₹{current:,.0f} ({percent_change:+.1f}% vs last month)"
            
            return {
                "success": True,
                "message": message,
                "insight_message": message,
                "current_month": current,
                "last_month": last,
                "percent_change": percent_change,
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Could not fetch insights: {str(e)}"
            }
    
    async def _execute_block_card(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Block card via wrapper endpoint"""
        
        headers = self._get_auth_headers(auth_token)
        
        # Use AI-optimized wrapper DTO
        payload = {
            "userId": int(user_id),
            "cardId": context.get("card_id"),
            "action": "BLOCK",
            "reason": context.get("reason", "Security block")
        }
        
        try:
            response = await self.client.post(
                f"{self.ai_base_url}/card/block",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            return {
                "success": True,
                "message": f"🔒 Card (****{data.get('cardLast4')}) has been blocked immediately for security",
                "card_id": data.get("cardId"),
                "card_last4": data.get("cardLast4"),
                "current_status": data.get("currentStatus"),
                "operation_id": data.get("operationId"),
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Could not block card: {str(e)}"
            }
    
    async def _execute_unblock_card(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Unblock card via wrapper endpoint"""
        
        headers = self._get_auth_headers(auth_token)
        
        # Use AI-optimized wrapper DTO
        payload = {
            "userId": int(user_id),
            "cardId": context.get("card_id"),
            "action": "UNBLOCK"
        }
        
        try:
            response = await self.client.post(
                f"{self.ai_base_url}/card/unblock",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            return {
                "success": True,
                "message": f"✅ Card (****{data.get('cardLast4')}) has been unblocked",
                "card_id": data.get("cardId"),
                "card_last4": data.get("cardLast4"),
                "current_status": data.get("currentStatus"),
                "operation_id": data.get("operationId"),
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Could not unblock card: {str(e)}"
            }
    
    async def _execute_schedule_transfer(
        self,
        context: Dict[str, Any],
        user_id: str,
        auth_token: str
    ) -> Dict[str, Any]:
        """Schedule transfer for future execution via wrapper endpoint"""
        
        headers = self._get_auth_headers(auth_token)
        
        # Use AI-optimized wrapper DTO
        payload = {
            "fromAccountId": context.get("from_account_id"),
            "toAccountId": context.get("to_account_id"),
            "amount": context.get("amount"),
            "amountFormatted": context.get("amount_formatted"),
            "recipient": context.get("recipient"),
            "purpose": context.get("purpose", "Scheduled transfer"),
            "userId": int(user_id)
        }
        
        try:
            response = await self.client.post(
                f"{self.ai_base_url}/transfer/schedule",
                json=payload,
                headers=headers
            )
            response.raise_for_status()
            
            data = response.json()
            return {
                "success": True,
                "message": f"✅ Transfer of ₹{context.get('amount')} to {context.get('recipient')} scheduled",
                "amount": context.get("amount"),
                "recipient": context.get("recipient"),
                "operation_id": data.get("operationId"),
                "requires_confirmation": data.get("requiresConfirmation"),
                "status": data.get("status"),
                "data": data
            }
        except httpx.HTTPError as e:
            return {
                "success": False,
                "message": f"Could not schedule transfer: {str(e)}"
            }
    
    def _get_auth_headers(self, auth_token: str) -> Dict[str, str]:
        """Get HTTP headers with authentication"""
        return {
            "Authorization": f"Bearer {auth_token}",
            "Content-Type": "application/json",
            "X-Request-Source": "ai-service"
        }
    
    async def close(self):
        """Close HTTP client"""
        await self.client.aclose()
