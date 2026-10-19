# bank-mcp-agent (Python)

The AI half of the banking assistant.
Spring Boot owns all rules, money and OTP. This layer owns the conversation.

## Structure

```
agent/
  config.py       — env-var config (copy .env.example → .env)
  bank_client.py  — async HTTP client for the Spring Boot API
  session.py      — in-memory session store (swap for Redis in prod)
  intent.py       — zero-temperature intent classifier (cheap isolated call)
  prompts.py      — system prompt + trilingual templates
  tools.py        — FastMCP tool definitions (12 tools)
  agent_loop.py   — LiteLLM agent loop with primary/fallback models
gateway.py        — FastAPI web server (POST /login, POST /chat)
mcp_server.py     — FastMCP server entry point (stdio or SSE)
tests/
  test_intent.py            — intent classifier (mocked LLM)
  test_tools.py             — tool layer (mocked BankClient)
  test_policy_integration.py — live API tests (set RUN_INTEGRATION=1)
```

## Setup (Windows PowerShell)

```powershell
cd mcp-agent
python -m venv .venv
.venv\Scripts\activate
pip install -e ".[mcp,dev]"

copy .env.example .env
# Edit .env: set GEMINI_API_KEY and confirm BANK_API_URL=http://127.0.0.1:8081
```

To run only the web gateway without the separate FastMCP server or test tools,
install `pip install -e .` instead. The MCP server extra is installed with
`.[mcp]`; development tests are installed with `.[dev]`.

## Run the gateway (web chat)

```powershell
python gateway.py
# Starts on http://localhost:8290
```

## Run the MCP server (Claude Desktop)

```powershell
pip install -e ".[mcp]"
python mcp_server.py          # stdio
# or
$env:MCP_TRANSPORT="sse"; python mcp_server.py   # SSE on port 8000
```

## Unit tests (no server needed)

```powershell
pytest tests/test_intent.py tests/test_tools.py -v
```

## Integration tests (requires running Spring Boot on 8081)

```powershell
$env:RUN_INTEGRATION="1"; pytest tests/test_policy_integration.py -v -s
```

## Try it with curl

```powershell
# 1. Login
$r = curl.exe -s -X POST http://localhost:8290/login -H "Content-Type: application/json" -d "{\"cif\":\"CIF0001\",\"lang\":\"en\"}" | ConvertFrom-Json
$sid = $r.session_id

# 2. Chat — ask a balance question
curl.exe -s -X POST http://localhost:8290/chat -H "Content-Type: application/json" -d "{\"session_id\":\"$sid\",\"message\":\"What's my balance?\"}"

# 3. Pay a bill
curl.exe -s -X POST http://localhost:8290/chat -H "Content-Type: application/json" -d "{\"session_id\":\"$sid\",\"message\":\"Pay my CEB bill\"}"

# 4. Try in Sinhala
curl.exe -s -X POST http://localhost:8290/chat -H "Content-Type: application/json" -d "{\"session_id\":\"$sid\",\"message\":\"CEB බිල් ගෙවන්න\"}"
```

## Run the full Docker stack

From the banking project root, add a valid `GEMINI_API_KEY` to its ignored
`.env` file alongside the admin settings, then run:

```powershell
docker compose up -d --build
```

The customer UI is at `http://localhost:8080`, the core API at
`http://localhost:8081`, and the gateway at `http://localhost:8290`. Without
a model key the gateway runs in `offline-read-only` mode: it can return live
account balances, loan details, and card status, but cannot process payments,
transfers, confirmations, or general questions. Add a valid `GEMINI_API_KEY`
to the ignored root `.env` to enable full AI conversations. Restart the
gateway after changing the root `.env` with
`docker compose up -d --force-recreate mcp-agent`. The `/health` response
reports both `ai_configured` and the active `mode`.

## Key design decisions

| Decision | Reason |
|---|---|
| Intent classifier is a separate LLM call (temperature=0) | Intelligence budget: cheap, testable, swappable |
| Tools return `next_action` hint | Response-as-instruction from your MCP course notes |
| Cancel is handled before touching the LLM | Failing forward: predictable, fast |
| Primary (gemini-2.5-flash-lite) → fallback (gemini-2.5-flash) | Automatic resilience via LiteLLM |
| Session stores pending state in memory | Simple for demo; Redis slot is clearly marked |
| BankClient forwards the customer JWT | AI never gets superuser — identity principle |
