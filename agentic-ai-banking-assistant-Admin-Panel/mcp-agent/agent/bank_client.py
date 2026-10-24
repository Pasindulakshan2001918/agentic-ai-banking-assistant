"""Thin async HTTP client that wraps every Spring Boot REST endpoint.
The agent only calls these — it never talks to the DB directly.
The customer's JWT is forwarded on every call so the AI acts with the
customer's own identity, never more (principle from your MCP notes).
"""
from __future__ import annotations

import uuid
from typing import Any

import httpx

from agent.config import BANK_API_URL

_TIMEOUT = httpx.Timeout(15.0)


class BankClient:
    def __init__(self, token: str, session_id: str, correlation_id: str | None = None, lang: str = "en"):
        self._token = token
        self._session_id = session_id
        self._correlation_id = correlation_id or str(uuid.uuid4())
        self._lang = lang

    def _headers(self) -> dict[str, str]:
        return {
            "Authorization": f"Bearer {self._token}",
            "Accept-Language": self._lang,
            "X-Session-Id": self._session_id,
            "X-Correlation-Id": self._correlation_id,
            "Content-Type": "application/json",
        }

    # ------------------------------------------------------------------ read
    async def get_me(self) -> dict:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/me", headers=self._headers())
            r.raise_for_status()
            return r.json()

    async def get_accounts(self) -> list[dict]:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/accounts", headers=self._headers())
            r.raise_for_status()
            return r.json()["accounts"]

    async def get_payees(self, q: str = "") -> list[dict]:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/payees", params={"q": q} if q else {}, headers=self._headers())
            r.raise_for_status()
            return r.json()["payees"]

    async def search_billers(self, q: str) -> list[dict]:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/billers", params={"q": q}, headers=self._headers())
            r.raise_for_status()
            return r.json()["billers"]

    async def get_registered_billers(self) -> list[dict]:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/billers/registered", headers=self._headers())
            r.raise_for_status()
            return r.json()["registeredBillers"]

    async def get_cards(self) -> list[dict]:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/cards", headers=self._headers())
            r.raise_for_status()
            return r.json()["cards"]

    async def get_loans(self) -> list[dict]:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/loans", headers=self._headers())
            r.raise_for_status()
            return r.json()["loans"]

    async def block_card(self, card_id: str) -> dict:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/cards/{card_id}/block", headers=self._headers())
            r.raise_for_status()
            return r.json()

    # ------------------------------------------------------------------ actions
    async def prepare(self, action_type: str, params: dict) -> dict:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/actions/prepare",
                             json={"actionType": action_type, "params": params},
                             headers=self._headers())
            r.raise_for_status()
            return r.json()

    async def confirm(self, pending_id: str, token: str, otp: str | None, idem_key: str) -> dict:
        h = {**self._headers(), "Idempotency-Key": idem_key}
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/actions/{pending_id}/confirm",
                             json={"confirmationToken": token, "otp": otp},
                             headers=h)
            r.raise_for_status()
            return r.json()

    async def resend_otp(self, pending_id: str) -> dict:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/actions/{pending_id}/otp/resend", headers=self._headers())
            r.raise_for_status()
            return r.json()

    async def cancel_action(self, pending_id: str) -> dict:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/actions/{pending_id}/cancel", headers=self._headers())
            r.raise_for_status()
            return r.json()

    async def get_receipt(self, txn_ref: str) -> dict:
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.get(f"{BANK_API_URL}/api/v1/receipts/{txn_ref}", headers=self._headers())
            r.raise_for_status()
            return r.json()

    # ------------------------------------------------------------------ audit
    async def audit(self, payload: dict) -> None:
        h = {**self._headers(), "X-Client": "mcp-agent"}
        try:
            async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
                await c.post(f"{BANK_API_URL}/api/v1/internal/audit", json=payload, headers=h)
        except Exception:
            pass   # audit failure must never break a banking call

    # ------------------------------------------------------------------ dev helper
    @classmethod
    async def dev_login(cls, cif: str, session_id: str, lang: str = "en") -> "BankClient":
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/auth/dev-login", json={"cif": cif})
            r.raise_for_status()
            data = r.json()
        return cls(token=data["accessToken"], session_id=session_id,
                   lang=data.get("preferredLanguage", "en").lower())

    @classmethod
    async def dev_signup(cls, cif: str, name: str, mobile: str, session_id: str, lang: str = "en") -> "BankClient":
        async with httpx.AsyncClient(timeout=_TIMEOUT) as c:
            r = await c.post(f"{BANK_API_URL}/api/v1/auth/dev-signup", json={"cif": cif, "name": name, "mobile": mobile, "lang": lang})
            r.raise_for_status()
            data = r.json()
        return cls(token=data["accessToken"], session_id=session_id,
                   lang=data.get("preferredLanguage", "en").lower())
