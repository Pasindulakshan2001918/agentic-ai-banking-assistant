"""Unit tests for MCP tools — mock the BankClient so no server needed."""
from __future__ import annotations

import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from agent.session import Session
from agent import tools


def _session(lang="en") -> Session:
    return Session(session_id="test-session", token="test-token", lang=lang)


# ── get_accounts ──────────────────────────────────────────────────────────────
@pytest.mark.asyncio
async def test_get_accounts_returns_list():
    mock_accounts = [
        {"id": "a1", "productType": "SAVINGS", "balance": "250000.00", "masked": "••••0001"},
    ]
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.get_accounts = AsyncMock(return_value=mock_accounts)
        MockClient.return_value = instance
        result = await tools.get_accounts(session=_session())
    assert result["count"] == 1
    assert result["accounts"][0]["productType"] == "SAVINGS"


# ── prepare_bill_payment: NEEDS_INFO when amount missing ─────────────────────
@pytest.mark.asyncio
async def test_prepare_bill_needs_info():
    java_response = {
        "decision": "NEEDS_INFO",
        "actionType": "BILL_PAYMENT",
        "missing": [
            {"field": "amount", "type": "money",
             "prompt": "How much?", "hint": "Due: LKR 5,000.00", "options": None}
        ],
    }
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.prepare = AsyncMock(return_value=java_response)
        MockClient.return_value = instance
        s = _session()
        result = await tools.prepare_bill_payment(
            session=s,
            from_account_id="a1",
            amount="",
            registered_biller_id="rb1",
        )
    assert result["status"] == "NEEDS_INFO"
    assert "amount" in result["missing_fields"]
    assert s.pending is None   # pending NOT set on NEEDS_INFO


# ── prepare_fund_transfer: ALLOW ──────────────────────────────────────────────
@pytest.mark.asyncio
async def test_prepare_transfer_allow():
    java_response = {
        "decision": "ALLOW",
        "actionType": "FUND_TRANSFER",
        "pendingActionId": "pid-1",
        "confirmationToken": "tok-1",
        "expiresAt": "2026-09-28T20:00:00+05:30",
        "summary": {"title": "Transfer LKR 1,000.00",
                    "lines": [{"label": "From", "value": "Main savings (••••0001)"},
                               {"label": "To",   "value": "Amma"},
                               {"label": "Amount", "value": "LKR 1,000.00"}]},
        "amount": "1000.00",
        "fee": "0.00",
        "otpRequired": False,
        "reasonCodes": ["OK"],
    }
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.prepare = AsyncMock(return_value=java_response)
        MockClient.return_value = instance
        s = _session()
        result = await tools.prepare_fund_transfer(
            session=s,
            from_account_id="a1",
            amount="1000.00",
            to_payee_id="p1",
        )
    assert result["status"] == "ALLOW"
    assert result["next_action"] == "confirm"
    assert s.pending is not None
    assert s.pending.pending_id == "pid-1"
    assert s.pending.otp_required is False


# ── prepare_fund_transfer: STEP_UP ────────────────────────────────────────────
@pytest.mark.asyncio
async def test_prepare_transfer_step_up():
    java_response = {
        "decision": "STEP_UP",
        "actionType": "FUND_TRANSFER",
        "pendingActionId": "pid-2",
        "confirmationToken": "tok-2",
        "expiresAt": "2026-09-28T20:00:00+05:30",
        "summary": {"title": "Transfer LKR 30,000.00", "lines": []},
        "amount": "30000.00",
        "fee": "0.00",
        "otpRequired": True,
        "reasonCodes": ["OTP_HIGH_VALUE"],
        "explanation": "OTP required because amount exceeds LKR 25,000.00.",
        "devOtp": "123456",
    }
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.prepare = AsyncMock(return_value=java_response)
        MockClient.return_value = instance
        s = _session()
        result = await tools.prepare_fund_transfer(
            session=s, from_account_id="a1", amount="30000.00", to_payee_id="p1"
        )
    assert result["status"] == "STEP_UP"
    assert result["next_action"] == "ask_otp"
    assert result.get("dev_otp") == "123456"
    assert s.pending.otp_required is True


# ── prepare_fund_transfer: DENY ───────────────────────────────────────────────
@pytest.mark.asyncio
async def test_prepare_transfer_deny():
    java_response = {
        "decision": "DENY",
        "actionType": "FUND_TRANSFER",
        "pendingActionId": None,
        "reasonCodes": ["INSUFFICIENT_FUNDS"],
        "explanation": "Your balance is not enough.",
    }
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.prepare = AsyncMock(return_value=java_response)
        MockClient.return_value = instance
        s = _session()
        result = await tools.prepare_fund_transfer(
            session=s, from_account_id="a1", amount="500000.00", to_payee_id="p1"
        )
    assert result["status"] == "DENY"
    assert s.pending is None


# ── confirm_action: COMPLETED ─────────────────────────────────────────────────
@pytest.mark.asyncio
async def test_confirm_action_completed():
    from agent.session import PendingState
    s = _session()
    s.pending = PendingState(
        pending_id="pid-1", confirmation_token="tok-1",
        action_type="FUND_TRANSFER", decision="ALLOW", otp_required=False,
        dev_otp=None, summary={}, amount="1000.00", fee="0.00",
        reason_codes=["OK"], explanation=None,
    )
    java_result = {
        "status": "COMPLETED",
        "message": "Transfer completed.",
        "transaction": {"txnRef": "TXN-20260928-100001", "amount": "1000.00", "completedAt": "2026-09-28T19:00:00+05:30"},
        "receipt": {"url": "/api/v1/receipts/TXN-20260928-100001"},
    }
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.confirm = AsyncMock(return_value=java_result)
        MockClient.return_value = instance
        result = await tools.confirm_action(session=s)
    assert result["status"] == "COMPLETED"
    assert result["txn_ref"] == "TXN-20260928-100001"
    assert s.pending is None   # cleared after completion


# ── confirm_action: OTP_INVALID ───────────────────────────────────────────────
@pytest.mark.asyncio
async def test_confirm_action_otp_invalid():
    from agent.session import PendingState
    s = _session()
    s.pending = PendingState(
        pending_id="pid-2", confirmation_token="tok-2",
        action_type="FUND_TRANSFER", decision="STEP_UP", otp_required=True,
        dev_otp=None, summary={}, amount="30000.00", fee="0.00",
        reason_codes=["OTP_HIGH_VALUE"], explanation=None,
    )
    java_result = {"status": "OTP_INVALID", "message": "That code is not correct.", "attemptsLeft": 2}
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.confirm = AsyncMock(return_value=java_result)
        MockClient.return_value = instance
        result = await tools.confirm_action(session=s, otp="999999")
    assert result["status"] == "OTP_INVALID"
    assert result["attempts_left"] == 2
    assert s.pending is not None   # NOT cleared — customer can try again


# ── cancel_action ─────────────────────────────────────────────────────────────
@pytest.mark.asyncio
async def test_cancel_action_clears_pending():
    from agent.session import PendingState
    s = _session()
    s.pending = PendingState(
        pending_id="pid-1", confirmation_token="tok-1",
        action_type="BILL_PAYMENT", decision="ALLOW", otp_required=False,
        dev_otp=None, summary={}, amount="5000.00", fee="0.00",
        reason_codes=["OK"], explanation=None,
    )
    with patch("agent.tools.BankClient") as MockClient:
        instance = AsyncMock()
        instance.cancel_action = AsyncMock(return_value={"status": "CANCELLED", "message": "Cancelled"})
        MockClient.return_value = instance
        result = await tools.cancel_action(session=s)
    assert result["status"] == "CANCELLED"
    assert s.pending is None


@pytest.mark.asyncio
async def test_cancel_action_no_pending():
    s = _session()
    result = await tools.cancel_action(session=s)
    assert result["status"] == "CANCELLED"
