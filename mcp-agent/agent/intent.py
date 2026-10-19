"""Zero-temperature intent classifier.
Isolated single-purpose LLM call — easy to unit test and to swap out.
From your course notes: 'self-prompting' / 'intelligence budget' — spend a
small focused call here rather than loading the main agent with classification.
"""
from __future__ import annotations

import json
import time
import logging

import litellm

from agent.config import PRIMARY_MODEL, FALLBACK_MODEL, GEMINI_API_KEY
from agent.prompts import INTENT_CLASSIFIER_PROMPT

log = logging.getLogger(__name__)

if GEMINI_API_KEY:
    litellm.api_key = GEMINI_API_KEY   # used by gemini/* models


async def classify(text: str) -> dict:
    """Returns {intent, confidence, language, slots}. Never raises — returns OTHER on any error."""
    t0 = time.monotonic()
    for model in (PRIMARY_MODEL, FALLBACK_MODEL):
        try:
            resp = await litellm.acompletion(
                model=model,
                temperature=0,
                max_tokens=200,
                messages=[
                    {"role": "system", "content": INTENT_CLASSIFIER_PROMPT},
                    {"role": "user",   "content": text},
                ],
            )
            raw = resp.choices[0].message.content.strip()
            # strip ```json fences if present
            if raw.startswith("```"):
                raw = raw.split("```")[1]
                if raw.startswith("json"):
                    raw = raw[4:]
            result = json.loads(raw)
            ms = int((time.monotonic() - t0) * 1000)
            log.debug("intent=%s conf=%.2f lang=%s model=%s latency=%dms",
                      result.get("intent"), result.get("confidence"), result.get("language"), model, ms)
            return result
        except json.JSONDecodeError as e:
            log.warning("Intent JSON parse failed (model=%s): %s", model, e)
        except Exception as e:
            log.warning("Intent classify error (model=%s): %s", model, e)
    return {"intent": "OTHER", "confidence": 0.0, "language": "en", "slots": {}}
