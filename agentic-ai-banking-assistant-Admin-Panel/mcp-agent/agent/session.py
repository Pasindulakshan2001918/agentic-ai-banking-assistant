"""In-memory session store.
Each chat session keeps: the customer JWT, the active pending action (if any),
conversation history for the LLM, and the language.
In production replace with Redis; the interface is the same.
"""
from __future__ import annotations

import uuid
from dataclasses import dataclass, field
from typing import Optional


@dataclass
class PendingState:
    """Tracks an in-flight prepare/confirm cycle."""
    pending_id: str
    confirmation_token: str
    action_type: str
    decision: str          # ALLOW or STEP_UP
    otp_required: bool
    dev_otp: Optional[str]
    summary: dict
    amount: Optional[str]
    fee: Optional[str]
    reason_codes: list[str]
    explanation: Optional[str]


@dataclass
class Session:
    session_id: str
    token: str
    lang: str                          # "en" | "si" | "ta"
    history: list[dict] = field(default_factory=list)
    pending: Optional[PendingState] = None
    customer_name: Optional[str] = None

    def add_user(self, text: str) -> None:
        self.history.append({"role": "user", "content": text})

    def add_assistant(self, text: str) -> None:
        self.history.append({"role": "assistant", "content": text})

    def clear_pending(self) -> None:
        self.pending = None

    def recent_history(self, n: int = 20) -> list[dict]:
        """Keep history bounded so the context window stays sane."""
        return self.history[-n:]


_store: dict[str, Session] = {}


def create(token: str, lang: str, customer_name: str | None = None) -> Session:
    sid = str(uuid.uuid4())
    s = Session(session_id=sid, token=token, lang=lang, customer_name=customer_name)
    _store[sid] = s
    return s


def get(session_id: str) -> Session | None:
    return _store.get(session_id)


def get_or_create(session_id: str, token: str, lang: str) -> Session:
    if session_id in _store:
        return _store[session_id]
    s = Session(session_id=session_id, token=token, lang=lang)
    _store[session_id] = s
    return s
