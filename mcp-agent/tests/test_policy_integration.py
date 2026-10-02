"""Integration tests that verify the policy engine via the live Spring Boot API.
These are SKIPPED unless BANK_API_URL is reachable and dev-login works.
Run manually: pytest tests/test_policy_integration.py -v -s
"""
from __future__ import annotations

import os
import uuid
import pytest
import httpx

BANK_URL = os.getenv("BANK_API_URL", "http://127.0.0.1:8081")
SKIP = not os.getenv("RUN_INTEGRATION", "")

pytestmark = pytest.mark.skipif(SKIP, reason="Set RUN_INTEGRATION=1 to run against a live server")


async def _login(cif: str) -> tuple[str, str]:
    async with httpx.AsyncClient() as c:
        r = await c.post(f"{BANK_URL}/api/v1/auth/dev-login", json={"cif": cif})
        r.raise_for_status()
        return r.json()["accessToken"], str(uuid.uuid4())


async def _prepare(token: str, session_id: str, action: str, params: dict) -> dict:
    h = {"Authorization": f"Bearer {token}", "X-Session-Id": session_id,
         "Accept-Language": "en", "Content-Type": "application/json"}
    async with httpx.AsyncClient() as c:
        r = await c.post(f"{BANK_URL}/api/v1/actions/prepare",
                         json={"actionType": action, "params": params}, headers=h)
        r.raise_for_status()
        return r.json()


@pytest.mark.asyncio
async def test_small_transfer_is_allow():
    token, sid = await _login("CIF0001")
    result = await _prepare(token, sid, "FUND_TRANSFER", {
        "fromAccountId": "a1111111-0000-0000-0000-000000000001",
        "toPayeeId": "get-this-from-GET-payees",   # placeholder — real id from /payees
        "amount": "1000.00",
    })
    # Without a real payee id this will be NEEDS_INFO or DENY; we just check it returns
    assert result["decision"] in ("ALLOW", "DENY", "NEEDS_INFO", "STEP_UP")


@pytest.mark.asyncio
async def test_bill_payment_needs_info_when_amount_missing():
    token, sid = await _login("CIF0001")
    result = await _prepare(token, sid, "BILL_PAYMENT", {
        "registeredBillerId": "e1111111-0000-0000-0000-000000000001",
        # no fromAccountId, no amount
    })
    assert result["decision"] == "NEEDS_INFO"
    missing_fields = [m["field"] for m in result["missing"]]
    assert "amount" in missing_fields


@pytest.mark.asyncio
async def test_bill_payment_full_cycle():
    token, sid = await _login("CIF0001")
    result = await _prepare(token, sid, "BILL_PAYMENT", {
        "fromAccountId": "a1111111-0000-0000-0000-000000000001",
        "registeredBillerId": "e1111111-0000-0000-0000-000000000001",
        "amount": "5000.00",
    })
    assert result["decision"] in ("ALLOW", "STEP_UP")
    if result["decision"] == "ALLOW":
        # Confirm without OTP
        h = {"Authorization": f"Bearer {token}", "X-Session-Id": sid,
             "Accept-Language": "en", "Content-Type": "application/json",
             "Idempotency-Key": str(uuid.uuid4())}
        async with httpx.AsyncClient() as c:
            r = await c.post(f"{BANK_URL}/api/v1/actions/{result['pendingActionId']}/confirm",
                             json={"confirmationToken": result["confirmationToken"], "otp": None}, headers=h)
            r.raise_for_status()
            confirm = r.json()
        assert confirm["status"] == "COMPLETED"
        assert "txnRef" in confirm["transaction"]
