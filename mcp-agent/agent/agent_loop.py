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
import re
import time
from decimal import Decimal, InvalidOperation
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
    {
        "type": "function",
        "function": {
            "name": "get_cards",
            "description": "Get customer's credit and debit cards.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_loans",
            "description": "Get customer's loans.",
            "parameters": {"type": "object", "properties": {}, "required": []},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "block_card",
            "description": "Block a specific card.",
            "parameters": {"type": "object", "properties": {
                "card_id": {"type": "string"}
            }, "required": ["card_id"]},
        },
    }
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
        "get_cards":             tool_module.get_cards,
        "get_loans":             tool_module.get_loans,
        "block_card":            tool_module.block_card,
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


def _offline_language(message: str, fallback: str) -> str:
    if re.search(r"[\u0D80-\u0DFF]", message):
        return "si"
    if re.search(r"[\u0B80-\u0BFF]", message):
        return "ta"
    return fallback if fallback in ("en", "si", "ta") else "en"


def _offline_intent(message: str) -> str | None:
    text = message.casefold()
    if any(word in text for word in ("balance", "ශේෂය", "இருப்பு")):
        return "accounts"
    if any(word in text for word in ("loan", "loans", "lease", "ණය", "கடன்")):
        return "loans"
    if any(word in text for word in ("card", "cards", "කාඩ්", "அட்டை")):
        return "cards"
    if any(word in text for word in ("account", "accounts", "ගිණුම", "கணக்கு")):
        return "accounts"
    if text.strip() in {"hello", "hi", "hey", "hello!", "hi!", "hey!", "thanks", "thank you",
                        "ආයුබෝවන්", "வணக்கம்"}:
        return "greeting"
    return None


def _offline_money(value: Any, currency: str = "LKR") -> str:
    try:
        amount = Decimal(str(value))
    except (InvalidOperation, TypeError, ValueError) as error:
        raise ValueError("Bank API returned an invalid monetary value") from error
    if not amount.is_finite():
        raise ValueError("Bank API returned a non-finite monetary value")
    return f"{currency} {amount:,.2f}"


def _offline_no_data(lang: str, kind: str) -> str:
    messages = {
        "en": {"accounts": "No accounts were found.", "loans": "No loans were found.", "cards": "No cards were found."},
        "si": {"accounts": "ගිණුම් හමු නොවීය.", "loans": "ණය හමු නොවීය.", "cards": "කාඩ්පත් හමු නොවීය."},
        "ta": {"accounts": "கணக்குகள் எதுவும் இல்லை.", "loans": "கடன்கள் எதுவும் இல்லை.", "cards": "அட்டைகள் எதுவும் இல்லை."},
    }
    return messages[lang][kind]


async def _offline_chat(user_message: str, session: Session) -> str:
    """Serve safe, read-only banking queries when no generative model is configured."""
    lang = _offline_language(user_message, session.lang)
    session.lang = lang
    intent = _offline_intent(user_message)

    if session.pending:
        reply = {
            "en": "For your security, pending transactions cannot be confirmed or changed while AI chat is unavailable. Please configure the AI provider and start a new session.",
            "si": "ඔබගේ ආරක්ෂාව සඳහා, AI සේවාව නොමැති විට බලාපොරොත්තු වන ගනුදෙනුවක් තහවුරු කිරීමට හෝ වෙනස් කිරීමට නොහැක. AI සේවාව සකසා නව සැසියක් ආරම්භ කරන්න.",
            "ta": "உங்கள் பாதுகாப்பிற்காக, AI சேவை இல்லாதபோது நிலுவையிலுள்ள பரிவர்த்தனையை உறுதிப்படுத்தவோ மாற்றவோ முடியாது. AI சேவையை அமைத்து புதிய அமர்வைத் தொடங்குங்கள்.",
        }[lang]
    elif intent == "greeting":
        reply = {
            "en": f"Hello {session.customer_name or 'there'}! I can look up your account balances, loan details, or card status. Payments and transfers need the AI service.",
            "si": f"ආයුබෝවන් {session.customer_name or ''}! ඔබගේ ගිණුම් ශේෂ, ණය තොරතුරු හෝ කාඩ්පත් තත්ත්වය පරීක්ෂා කළ හැක. ගෙවීම් සහ මුදල් මාරු කිරීමට AI සේවාව අවශ්‍යයි.",
            "ta": f"வணக்கம் {session.customer_name or ''}! உங்கள் கணக்கு இருப்புகள், கடன் விவரங்கள் அல்லது அட்டை நிலையைப் பார்க்கலாம். கட்டணங்கள் மற்றும் பணப் பரிமாற்றங்களுக்கு AI சேவை தேவை.",
        }[lang]
    elif intent in ("accounts", "loans", "cards"):
        result = await {
            "accounts": tool_module.get_accounts,
            "loans": tool_module.get_loans,
            "cards": tool_module.get_cards,
        }[intent](session=session)
        if "error" in result:
            log.error("offline %s lookup failed: %s", intent, result["error"])
            reply = {
                "en": "I couldn't retrieve that information from the banking service. Please try again.",
                "si": "බැංකු සේවාවෙන් එම තොරතුරු ලබාගත නොහැකි විය. නැවත උත්සාහ කරන්න.",
                "ta": "வங்கி சேவையிலிருந்து அந்தத் தகவலைப் பெற முடியவில்லை. மீண்டும் முயற்சிக்கவும்.",
            }[lang]
        elif intent == "accounts":
            accounts = result.get("accounts", [])
            if not accounts:
                reply = _offline_no_data(lang, intent)
            else:
                heading = {"en": "Your account balances:", "si": "ඔබගේ ගිණුම් ශේෂයන්:", "ta": "உங்கள் கணக்கு இருப்புகள்:"}[lang]
                lines = [
                    f"{item.get('nickname') or item.get('productType') or 'Account'} "
                    f"({item.get('masked') or 'masked number unavailable'}): "
                    f"{_offline_money(item.get('balance'), item.get('currency') or 'LKR')}"
                    for item in accounts
                ]
                reply = heading + "\n" + "\n".join(lines)
        elif intent == "loans":
            loans = result.get("loans", [])
            if not loans:
                reply = _offline_no_data(lang, intent)
            else:
                heading = {"en": "Your loans:", "si": "ඔබගේ ණය:", "ta": "உங்கள் கடன்கள்:"}[lang]
                lines = [
                    f"{item.get('product_type') or item.get('loan_no') or 'Loan'}: "
                    f"outstanding {_offline_money(item.get('outstanding'))}; "
                    f"original principal {_offline_money(item.get('principal'))}"
                    for item in loans
                ]
                reply = heading + "\n" + "\n".join(lines)
        else:
            cards = result.get("cards", [])
            if not cards:
                reply = _offline_no_data(lang, intent)
            else:
                heading = {"en": "Your cards:", "si": "ඔබගේ කාඩ්පත්:", "ta": "உங்கள் அட்டைகள்:"}[lang]
                lines = [
                    f"{item.get('card_type') or 'Card'} {item.get('network') or ''} "
                    f"ending in {item.get('card_last4') or '••••'}: {item.get('status') or 'status unavailable'}"
                    for item in cards
                ]
                reply = heading + "\n" + "\n".join(lines)
    else:
        reply = {
            "en": "AI chat is not configured yet. Without it, I can only look up account balances, loan details, and card status. Payments and transfers are unavailable.",
            "si": "AI සංවාදය තවම සකසා නැත. එය නොමැතිව ගිණුම් ශේෂ, ණය තොරතුරු සහ කාඩ්පත් තත්ත්වය පමණක් පරීක්ෂා කළ හැක. ගෙවීම් සහ මුදල් මාරු කළ නොහැක.",
            "ta": "AI உரையாடல் இன்னும் அமைக்கப்படவில்லை. அது இல்லாமல் கணக்கு இருப்புகள், கடன் விவரங்கள் மற்றும் அட்டை நிலையை மட்டுமே பார்க்க முடியும். கட்டணங்கள் மற்றும் பணப் பரிமாற்றங்கள் கிடைக்காது.",
        }[lang]

    session.add_user(user_message)
    session.add_assistant(reply)
    return reply


# ── Main entry point ─────────────────────────────────────────────────────────

async def chat(user_message: str, session: Session) -> str:
    """Process one user turn and return the assistant reply.
    The agent loop runs until the LLM stops calling tools (max 8 iterations to prevent runaway).
    """
    if not GEMINI_API_KEY:
        return await _offline_chat(user_message, session)

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
