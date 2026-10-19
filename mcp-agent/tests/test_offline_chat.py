from __future__ import annotations

from unittest.mock import AsyncMock, patch

import pytest

from agent.agent_loop import chat
from agent.session import PendingState, Session


def _session(lang: str = "en") -> Session:
    return Session(session_id="offline-session", token="customer-token", lang=lang, customer_name="Nimal")


@pytest.mark.asyncio
async def test_offline_balance_uses_live_account_data():
    session = _session()
    account_data = {
        "accounts": [
            {"nickname": "Main Savings", "masked": "••••0001", "currency": "LKR", "balance": "250000.00"},
        ],
        "count": 1,
    }
    with (
        patch("agent.agent_loop.GEMINI_API_KEY", ""),
        patch("agent.agent_loop.classify", new=AsyncMock(side_effect=AssertionError("LLM classifier called"))),
        patch("agent.agent_loop.tool_module.get_accounts", new=AsyncMock(return_value=account_data)),
    ):
        reply = await chat("Check balance", session)

    assert "Main Savings (••••0001): LKR 250,000.00" in reply
    assert session.history[-2:] == [
        {"role": "user", "content": "Check balance"},
        {"role": "assistant", "content": reply},
    ]


@pytest.mark.asyncio
async def test_offline_balance_detects_sinhala_and_uses_localized_heading():
    session = _session()
    account_data = {"accounts": [{"nickname": "Savings", "masked": "••••0001", "currency": "LKR", "balance": "1000"}]}
    with (
        patch("agent.agent_loop.GEMINI_API_KEY", ""),
        patch("agent.agent_loop.tool_module.get_accounts", new=AsyncMock(return_value=account_data)),
    ):
        reply = await chat("මගේ ගිණුමේ ශේෂය කීයද?", session)

    assert reply.startswith("ඔබගේ ගිණුම් ශේෂයන්:")
    assert session.lang == "si"
    assert "LKR 1,000.00" in reply


@pytest.mark.asyncio
async def test_offline_unknown_request_explains_limited_support():
    session = _session()
    with (
        patch("agent.agent_loop.GEMINI_API_KEY", ""),
        patch("agent.agent_loop._llm", new=AsyncMock(side_effect=AssertionError("LLM called"))),
    ):
        reply = await chat("Pay my electricity bill", session)

    assert "AI chat is not configured yet" in reply
    assert "Payments and transfers are unavailable" in reply


@pytest.mark.asyncio
async def test_offline_mode_never_confirms_pending_transactions():
    session = _session()
    session.pending = PendingState(
        pending_id="pending-1",
        confirmation_token="confirmation-token",
        action_type="FUND_TRANSFER",
        decision="ALLOW",
        otp_required=False,
        dev_otp=None,
        summary={},
        amount="1000",
        fee="0",
        reason_codes=[],
        explanation=None,
    )
    with (
        patch("agent.agent_loop.GEMINI_API_KEY", ""),
        patch("agent.agent_loop.tool_module.confirm_action", new=AsyncMock(side_effect=AssertionError("Confirmed without AI"))),
        patch("agent.agent_loop.tool_module.cancel_action", new=AsyncMock(side_effect=AssertionError("Changed transaction without AI"))),
    ):
        reply = await chat("yes", session)

    assert "cannot be confirmed or changed" in reply
    assert session.pending is not None


@pytest.mark.asyncio
async def test_offline_malformed_account_amount_is_not_reported_as_a_balance():
    session = _session()
    with (
        patch("agent.agent_loop.GEMINI_API_KEY", ""),
        patch("agent.agent_loop.tool_module.get_accounts", new=AsyncMock(return_value={
            "accounts": [{"nickname": "Savings", "masked": "••••0001", "currency": "LKR", "balance": "not-a-number"}],
        })),
    ):
        with pytest.raises(ValueError, match="invalid monetary value"):
            await chat("Check balance", session)
