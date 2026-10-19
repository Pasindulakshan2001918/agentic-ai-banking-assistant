"""Unit tests for the intent classifier.
These run without a running LLM — we mock litellm.acompletion.
"""
from __future__ import annotations

import json
import pytest
from unittest.mock import AsyncMock, patch, MagicMock

from agent.intent import classify


def _mock_response(payload: dict):
    msg = MagicMock()
    msg.content = json.dumps(payload)
    choice = MagicMock()
    choice.message = msg
    resp = MagicMock()
    resp.choices = [choice]
    return resp


@pytest.mark.asyncio
async def test_english_transfer_intent():
    result_payload = {"intent": "FUND_TRANSFER", "confidence": 0.97,
                      "language": "en", "slots": {"amount": "5000", "payee": "Amma"}}
    with patch("litellm.acompletion", new=AsyncMock(return_value=_mock_response(result_payload))):
        r = await classify("Transfer 5000 to Amma")
    assert r["intent"] == "FUND_TRANSFER"
    assert r["confidence"] >= 0.9
    assert r["slots"]["amount"] == "5000"


@pytest.mark.asyncio
async def test_sinhala_bill_intent():
    result_payload = {"intent": "BILL_PAYMENT", "confidence": 0.93,
                      "language": "si", "slots": {"biller": "CEB"}}
    with patch("litellm.acompletion", new=AsyncMock(return_value=_mock_response(result_payload))):
        r = await classify("CEB බිල් ගෙවන්න")
    assert r["intent"] == "BILL_PAYMENT"
    assert r["language"] == "si"


@pytest.mark.asyncio
async def test_tamil_balance_intent():
    result_payload = {"intent": "CHECK_BALANCE", "confidence": 0.95,
                      "language": "ta", "slots": {}}
    with patch("litellm.acompletion", new=AsyncMock(return_value=_mock_response(result_payload))):
        r = await classify("என் கணக்கு இருப்பு என்ன?")
    assert r["intent"] == "CHECK_BALANCE"
    assert r["language"] == "ta"


@pytest.mark.asyncio
async def test_otp_submit_intent():
    result_payload = {"intent": "OTP_SUBMIT", "confidence": 0.99,
                      "language": "en", "slots": {}}
    with patch("litellm.acompletion", new=AsyncMock(return_value=_mock_response(result_payload))):
        r = await classify("123456")
    assert r["intent"] == "OTP_SUBMIT"


@pytest.mark.asyncio
async def test_confirm_yes():
    result_payload = {"intent": "CONFIRM_YES", "confidence": 0.99,
                      "language": "en", "slots": {}}
    with patch("litellm.acompletion", new=AsyncMock(return_value=_mock_response(result_payload))):
        r = await classify("yes please go ahead")
    assert r["intent"] == "CONFIRM_YES"


@pytest.mark.asyncio
async def test_classifier_fallback_on_bad_json():
    """If LLM returns garbage, classifier should return OTHER without crashing."""
    bad_msg = MagicMock(); bad_msg.content = "I am an LLM and I feel like talking"
    bad_choice = MagicMock(); bad_choice.message = bad_msg
    bad_resp = MagicMock(); bad_resp.choices = [bad_choice]
    with patch("litellm.acompletion", new=AsyncMock(return_value=bad_resp)):
        r = await classify("hello")
    assert r["intent"] == "OTHER"
    assert r["confidence"] == 0.0


@pytest.mark.asyncio
async def test_classifier_fallback_on_error():
    """If both models fail, should return OTHER without raising."""
    with patch("litellm.acompletion", new=AsyncMock(side_effect=Exception("network error"))):
        r = await classify("transfer money")
    assert r["intent"] == "OTHER"
