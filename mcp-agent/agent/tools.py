"""FastMCP tool definitions — the only interface between the LLM and the bank.
Each tool is a thin wrapper: validate inputs, call BankClient, return a
structured dict the agent reads as response-as-instruction (your course notes).

Tool responses always carry:
  status      : NEEDS_INFO | ALLOW | STEP_UP | DENY | COMPLETED | ERROR
  tell_user   : ready-made sentence the agent can relay (already localised by Java)
  next_action : hint for the agent loop (prepare_again | confirm | ask_otp | done | none)
"""
from __future__ import annotations

import uuid
import logging
from typing import Any

from fastmcp import FastMCP

from agent.bank_client import BankClient
from agent.session import Session, PendingState

log = logging.getLogger(__name__)

mcp = FastMCP("bank-tools")


# ─────────────────────────────── helpers ──────────────────────────────────────

def _client(session: Session) -> BankClient:
    return BankClient(token=session.token, session_id=session.session_id, lang=session.lang)


def _pending_from_prepare(data: dict, session: Session) -> PendingState | None:
    if data["decision"] not in ("ALLOW", "STEP_UP"):
        return None
    return PendingState(
        pending_id=data["pendingActionId"],
        confirmation_token=data["confirmationToken"],
        action_type=data["actionType"],
        decision=data["decision"],
        otp_required=data.get("otpRequired", False),
        dev_otp=data.get("devOtp"),
        summary=data.get("summary") or {},
        amount=data.get("amount"),
        fee=data.get("fee"),
        reason_codes=data.get("reasonCodes", []),
        explanation=data.get("explanation"),
    )


def _prepare_response(data: dict, session: Session) -> dict:
    """Convert the Java prepare response into the agent-friendly shape."""
    decision = data["decision"]

    if decision == "NEEDS_INFO":
        missing = data.get("missing", [])
        fields = [m["field"] for m in missing]
        hints = {m["field"]: m.get("hint") or m.get("prompt", "") for m in missing}
        opts  = {m["field"]: m.get("options") or [] for m in missing}
        return {
            "status": "NEEDS_INFO",
            "missing_fields": fields,
            "field_hints": hints,
            "field_options": opts,
            "next_action": "prepare_again",
            "tell_user": None,   # agent composes the question naturally
        }

    if decision == "DENY":
        return {
            "status": "DENY",
            "reason_codes": data.get("reasonCodes", []),
            "tell_user": data.get("explanation") or "This transaction cannot be completed.",
            "next_action": "none",
        }

    # ALLOW or STEP_UP — store pending state in session
    ps = _pending_from_prepare(data, session)
    session.pending = ps

    summary = data.get("summary") or {}
    lines = summary.get("lines", [])
    summary_text = "  ".join(f"{l['label']}: {l['value']}" for l in lines)

    base = {
        "status": decision,
        "pending_id": data["pendingActionId"],
        "summary": summary_text,
        "amount": data.get("amount"),
        "fee": data.get("fee"),
        "reason_codes": data.get("reasonCodes", []),
        "explanation": data.get("explanation"),
        "next_action": "confirm" if decision == "ALLOW" else "ask_otp",
    }
    if decision == "STEP_UP":
        base["otp_required"] = True
        base["tell_user"] = (data.get("explanation") or "") + " Please enter the OTP sent to your phone."
        if data.get("devOtp"):          # dev only
            base["dev_otp"] = data["devOtp"]
    else:
        base["tell_user"] = None        # agent shows summary and asks yes/no
    return base


# ─────────────────────────────── read tools ───────────────────────────────────

@mcp.tool()
async def get_accounts(session: Session) -> dict:
    """Return the customer's accounts with balances."""
    try:
        accounts = await _client(session).get_accounts()
        return {"accounts": accounts, "count": len(accounts)}
    except Exception as e:
        log.error("get_accounts: %s", e)
        return {"error": str(e)}


@mcp.tool()
async def get_payees(session: Session, query: str = "") -> dict:
    """Search saved payees by name. Leave query empty to list all."""
    try:
        payees = await _client(session).get_payees(query)
        return {"payees": payees, "count": len(payees)}
    except Exception as e:
        return {"error": str(e)}


@mcp.tool()
async def search_billers(session: Session, query: str) -> dict:
    """Search billers by name or alias (e.g. 'ceb', 'water', 'dialog').
    Returns ranked matches. If score gap between top two is < 0.15 the agent should ask the customer to confirm which one."""
    try:
        billers = await _client(session).search_billers(query)
        return {"billers": billers, "count": len(billers)}
    except Exception as e:
        return {"error": str(e)}


@mcp.tool()
async def get_registered_billers(session: Session) -> dict:
    """Return the customer's saved billers with current dues and due dates."""
    try:
        rb = await _client(session).get_registered_billers()
        return {"registered_billers": rb, "count": len(rb)}
    except Exception as e:
        return {"error": str(e)}


# ─────────────────────────────── prepare tools ────────────────────────────────

@mcp.tool()
async def prepare_fund_transfer(
    session: Session,
    from_account_id: str,
    amount: str,
    to_payee_id: str | None = None,
    to_own_account_id: str | None = None,
    to_new_account_bank_code: str | None = None,
    to_new_account_no: str | None = None,
    to_new_account_name: str | None = None,
    remarks: str | None = None,
) -> dict:
    """Prepare a fund transfer. Provide exactly one destination:
    to_payee_id (saved payee), to_own_account_id (own account),
    or to_new_account_* (unsaved beneficiary).
    Returns NEEDS_INFO, ALLOW, STEP_UP, or DENY."""
    params: dict[str, Any] = {"fromAccountId": from_account_id, "amount": amount}
    if to_payee_id:
        params["toPayeeId"] = to_payee_id
    elif to_own_account_id:
        params["toOwnAccountId"] = to_own_account_id
    elif to_new_account_bank_code:
        params["toNewAccount"] = {
            "bankCode": to_new_account_bank_code,
            "accountNo": to_new_account_no,
            "name": to_new_account_name,
        }
    if remarks:
        params["remarks"] = remarks
    try:
        data = await _client(session).prepare("FUND_TRANSFER", params)
        return _prepare_response(data, session)
    except Exception as e:
        log.error("prepare_fund_transfer: %s", e)
        return {"status": "ERROR", "tell_user": "Something went wrong. Please try again.", "error": str(e)}


@mcp.tool()
async def prepare_bill_payment(
    session: Session,
    from_account_id: str,
    amount: str,
    registered_biller_id: str | None = None,
    biller_id: str | None = None,
    reference_no: str | None = None,
) -> dict:
    """Prepare a bill payment. Use registered_biller_id for a saved biller,
    or biller_id + reference_no for an ad-hoc payment.
    Returns NEEDS_INFO, ALLOW, STEP_UP, or DENY."""
    params: dict[str, Any] = {"fromAccountId": from_account_id, "amount": amount}
    if registered_biller_id:
        params["registeredBillerId"] = registered_biller_id
    elif biller_id:
        params["billerId"] = biller_id
        params["referenceNo"] = reference_no
    try:
        data = await _client(session).prepare("BILL_PAYMENT", params)
        return _prepare_response(data, session)
    except Exception as e:
        log.error("prepare_bill_payment: %s", e)
        return {"status": "ERROR", "tell_user": "Something went wrong. Please try again.", "error": str(e)}


# ─────────────────────────────── confirm / OTP / cancel ───────────────────────

@mcp.tool()
async def confirm_action(
    session: Session,
    otp: str | None = None,
) -> dict:
    """Confirm the currently pending action. Pass otp if the action requires step-up.
    Returns status=COMPLETED on success, or an error status the agent must relay."""
    ps = session.pending
    if not ps:
        return {"status": "ERROR", "tell_user": "There is no pending action to confirm."}
    idem_key = str(uuid.uuid4())
    try:
        result = await _client(session).confirm(ps.pending_id, ps.confirmation_token, otp, idem_key)
        status = result.get("status", "")
        if status == "COMPLETED":
            session.clear_pending()
            txn = result.get("transaction", {})
            receipt_url = result.get("receipt", {}).get("url", "")
            return {
                "status": "COMPLETED",
                "txn_ref": txn.get("txnRef"),
                "amount": txn.get("amount"),
                "completed_at": txn.get("completedAt"),
                "receipt_url": receipt_url,
                "tell_user": result.get("message", "Transaction completed."),
                "next_action": "done",
            }
        # Business failure (OTP_INVALID, OTP_EXPIRED, OTP_LOCKED, DENIED_ON_RECHECK)
        tell = result.get("message", status)
        next_act = "ask_otp" if status in ("OTP_INVALID",) else (
                   "resend_otp" if status == "OTP_EXPIRED" else "none")
        if status in ("OTP_LOCKED", "DENIED_ON_RECHECK"):
            session.clear_pending()
        return {
            "status": status,
            "tell_user": tell,
            "attempts_left": result.get("attemptsLeft"),
            "can_resend": result.get("canResend", False),
            "next_action": next_act,
        }
    except Exception as e:
        log.error("confirm_action: %s", e)
        return {"status": "ERROR", "tell_user": "Something went wrong during confirmation.", "error": str(e)}


@mcp.tool()
async def resend_otp(session: Session) -> dict:
    """Resend the OTP for the current pending action."""
    ps = session.pending
    if not ps:
        return {"status": "ERROR", "tell_user": "No pending action found."}
    try:
        result = await _client(session).resend_otp(ps.pending_id)
        r = {"status": "OTP_SENT", "tell_user": "A new OTP has been sent to your phone.", "next_action": "ask_otp"}
        if result.get("devOtp"):
            r["dev_otp"] = result["devOtp"]
        return r
    except Exception as e:
        return {"status": "ERROR", "tell_user": "Could not resend OTP.", "error": str(e)}


@mcp.tool()
async def cancel_action(session: Session) -> dict:
    """Cancel the current pending action. Call this when the customer says no or cancel."""
    ps = session.pending
    if not ps:
        session.clear_pending()
        return {"status": "CANCELLED", "tell_user": "Nothing to cancel.", "next_action": "none"}
    try:
        await _client(session).cancel_action(ps.pending_id)
    except Exception:
        pass   # cancel best-effort; clear locally regardless
    session.clear_pending()
    return {"status": "CANCELLED", "tell_user": "Request cancelled.", "next_action": "none"}


@mcp.tool()
async def get_receipt(session: Session, txn_ref: str) -> dict:
    """Fetch a formatted receipt for a completed transaction."""
    try:
        receipt = await _client(session).get_receipt(txn_ref)
        lines = "  |  ".join(f"{l['label']}: {l['value']}" for l in receipt.get("lines", []))
        return {
            "title": receipt.get("title"),
            "lines": lines,
            "footer": receipt.get("footer"),
            "completed_at": receipt.get("completedAt"),
        }
    except Exception as e:
        return {"error": str(e)}
