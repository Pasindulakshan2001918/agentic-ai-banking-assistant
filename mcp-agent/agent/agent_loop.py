"""Core agent loop.
Implements the pattern from your course notes:
  - response-as-instruction  (tool returns next_action hint)
  - failing forward          (every error produces a next step, not a crash)
  - intelligence budget      (intent classifier is a separate cheap call)
  - identity                 (JWT forwarded; AI never gets superuser)
"""
from __future__ import annotations

import json
import logging
import time
from typing import Any

import litellm

from agent.config import PRIMARY_MODEL, FALLBACK_MODEL, GEMINI_API_KEY
from agent.intent import classify
from agent.prompts import SYSTEM_PROMPT, LANG_CANCELLED, LANG_CONFIRM_PROMPT, LANG_OTP_PROMPT
from agent.session import Session
from agent import tools as tool_module

log = logging.getLogger(__name__)

if GEMINI_API_KEY:
    litellm.api_key = GEMINI_API_KEY

# ── Tool schema the LLM sees (function-calling format) ─────────────────────
# We expose a curated subset; the full FastMCP server is for Claude Desktop.
# The gateway uses raw function-calling so it works with any LiteLLM model.

_TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "get_accounts",
            "description": "Get customer's accounts with balances.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_payees",
            "description": "Search saved payees. Leave query empty to list all.",
            "parameters": {"type": "object", "properties": {
                "query": {"type": "string", "default": ""}
            }, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "search_billers",
            "description": "Search billers by name or alias (ceb, water, dialog…).",
            "parameters": {"type": "object", "properties": {
                "query": {"type": "string"}
            }, "required": ["query"]},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_registered_billers",
            "description": "List the customer's saved billers with current dues.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "prepare_fund_transfer",
            "description": "Prepare a fund transfer. Returns NEEDS_INFO, ALLOW, STEP_UP or DENY.",
            "parameters": {"type": "object", "properties": {
                "from_account_id":          {"type": "string"},
                "amount":                   {"type": "string"},
                "to_payee_id":              {"type": "string"},
                "to_own_account_id":        {"type": "string"},
                "to_new_account_bank_code": {"type": "string"},
                "to_new_account_no":        {"type": "string"},
                "to_new_account_name":      {"type": "string"},
                "remarks":                  {"type": "string"},
            }, "required": ["from_account_id", "amount"]},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "prepare_bill_payment",
            "description": "Prepare a bill payment. Returns NEEDS_INFO, ALLOW, STEP_UP or DENY.",
            "parameters": {"type": "object", "properties": {
                "from_account_id":      {"type": "string"},
                "amount":               {"type": "string"},
                "registered_biller_id": {"type": "string"},
                "biller_id":            {"type": "string"},
                "reference_no":         {"type": "string"},
            }, "required": ["from_account_id", "amount"]},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "confirm_action",
            "description": "Confirm the pending action. Pass otp for STEP_UP actions.",
            "parameters": {"type": "object", "properties": {
                "otp": {"type": "string"},
            }, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "resend_otp",
            "description": "Resend OTP for the current pending action.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "cancel_action",
            "description": "Cancel the current pending action.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_receipt",
            "description": "Fetch receipt for a completed transaction.",
            "parameters": {"type": "object", "properties": {
                "txn_ref": {"type": "string"}
            }, "required": ["txn_ref"]},
        },
    },
]

# ── Tool dispatch ────────────────────────────────────────────────────────────

async def _dispatch(name: str, args: dict, session: Session) -> Any:
    """Call the right tool function, always injecting the session."""
    fn_map = {
        "get_accounts":          tool_module.get_accounts,
        "get_payees":            tool_module.get_payees,
        "search_billers":        tool_module.search_billers,
        "get_registered_billers":tool_module.get_registered_billers,
        "prepare_fund_transfer": tool_module.prepare_fund_transfer,
        "prepare_bill_payment":  tool_module.prepare_bill_payment,
        "confirm_action":        tool_module.confirm_action,
        "resend_otp":            tool_module.resend_otp,
        "cancel_action":         tool_module.cancel_action,
        "get_receipt":           tool_module.get_receipt,
    }
    fn = fn_map.get(name)
    if fn is None:
        return {"error": f"Unknown tool: {name}"}
    # All tool functions accept session as first positional arg
    return await fn(session=session, **args)

# ── LLM call with primary → fallback ─────────────────────────────────────────

async def _llm(messages: list[dict], session: Session) -> Any:
    """Call the LLM with automatic fallback. Returns the response object."""
    for model in (PRIMARY_MODEL, FALLBACK_MODEL):
        try:
            t0 = time.monotonic()
            resp = await litellm.acompletion(
                model=model,
                messages=messages,
                tools=_TOOLS,
                tool_choice="auto",
                temperature=0.3,
                max_tokens=1024,
            )
            ms = int((time.monotonic() - t0) * 1000)
            log.debug("llm model=%s latency=%dms", model, ms)
            return resp
        except Exception as e:
            log.warning("LLM call failed (model=%s): %s", model, e)
    raise RuntimeError("Both primary and fallback models failed.")

# ── Main entry point ─────────────────────────────────────────────────────────

async def chat(user_message: str, session: Session) -> str:
    """Process one user turn and return the assistant reply.
    The agent loop runs until the LLM stops calling tools (max 8 iterations to prevent runaway).
    """
    # 1. Classify intent (cheap isolated call — intelligence budget)
    intent_data = await classify(user_message)
    intent      = intent_data.get("intent", "OTHER")
    lang        = intent_data.get("language", session.lang)
    session.lang = lang   # honour language switch mid-conversation

    log.info("turn intent=%s lang=%s", intent, lang)

    # 2. Handle CANCEL shortcut before touching the LLM
    if intent == "CANCEL" and session.pending:
        result = await tool_module.cancel_action(session=session)
        reply = LANG_CANCELLED.get(lang, LANG_CANCELLED["en"])
        session.add_user(user_message)
        session.add_assistant(reply)
        return reply

    # 3. Build message list for the LLM
    messages: list[dict] = [{"role": "system", "content": SYSTEM_PROMPT}]
    messages += session.recent_history(n=20)
    messages.append({"role": "user", "content": user_message})

    # 4. Agentic loop
    MAX_ITERS = 8
    for i in range(MAX_ITERS):
        resp = await _llm(messages, session)
        msg  = resp.choices[0].message

        # No tool call → LLM produced a final text reply
        if not msg.tool_calls:
            reply = msg.content or ""
            session.add_user(user_message if i == 0 else "")
            session.add_assistant(reply)
            return reply

        # Append assistant message with tool calls
        messages.append({"role": "assistant", "content": msg.content, "tool_calls": [
            {"id": tc.id, "type": "function",
             "function": {"name": tc.function.name, "arguments": tc.function.arguments}}
            for tc in msg.tool_calls
        ]})

        # Execute every tool call in this turn
        for tc in msg.tool_calls:
            fn_name = tc.function.name
            try:
                args = json.loads(tc.function.arguments or "{}")
            except json.JSONDecodeError:
                args = {}

            t0 = time.monotonic()
            result = await _dispatch(fn_name, args, session)
            ms = int((time.monotonic() - t0) * 1000)
            log.debug("tool=%s latency=%dms status=%s", fn_name, ms, result.get("status") if isinstance(result, dict) else "ok")

            messages.append({
                "role": "tool",
                "tool_call_id": tc.id,
                "content": json.dumps(result, default=str),
            })

    # Safety net: if loop exhausted without a text reply
    reply = "I'm sorry, I wasn't able to complete that request. Please try again."
    session.add_user(user_message)
    session.add_assistant(reply)
    return reply
