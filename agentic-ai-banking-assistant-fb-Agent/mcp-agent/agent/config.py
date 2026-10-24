"""Central config loaded from environment / .env file."""
from __future__ import annotations

import os
from pathlib import Path
from dotenv import load_dotenv

load_dotenv(Path(__file__).parent.parent / ".env", override=False)

def _require(key: str) -> str:
    v = os.getenv(key, "").strip()
    if not v:
        raise RuntimeError(f"Required env var {key} is not set. Copy .env.example → .env and fill it in.")
    return v

BANK_API_URL  = os.getenv("BANK_API_URL", "http://127.0.0.1:8081").rstrip("/")
PRIMARY_MODEL = os.getenv("PRIMARY_MODEL", "gemini/gemini-2.0-flash-lite")
FALLBACK_MODEL= os.getenv("FALLBACK_MODEL", "gemini/gemini-1.5-flash-latest")
GEMINI_API_KEY= os.getenv("GEMINI_API_KEY", "")

MCP_TRANSPORT = os.getenv("MCP_TRANSPORT", "stdio")
MCP_HOST      = os.getenv("MCP_HOST", "0.0.0.0")
MCP_PORT      = int(os.getenv("MCP_PORT", "8000"))

GATEWAY_HOST  = os.getenv("GATEWAY_HOST", "0.0.0.0")
GATEWAY_PORT  = int(os.getenv("GATEWAY_PORT", "8290"))

# If set, every LiteLLM call is routed through a proxy (e.g. LiteLLM Proxy Server)
LITELLM_API_BASE = os.getenv("LITELLM_API_BASE", "")
