"""HTTP gateway — the web chat frontend talks to this.
POST /chat      : send a message, get a reply
POST /login     : dev login, returns session_id
GET  /session   : session info (for the UI header)
"""
from __future__ import annotations

import asyncio
import logging
import uuid
from contextlib import asynccontextmanager

from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from pydantic import BaseModel

from agent import session as session_store
from agent.agent_loop import chat
from agent.bank_client import BankClient
from agent.config import GATEWAY_HOST, GATEWAY_PORT

logging.basicConfig(level=logging.INFO, format="%(asctime)s %(levelname)s %(name)s: %(message)s")
log = logging.getLogger(__name__)


@asynccontextmanager
async def lifespan(app: FastAPI):
    log.info("Bank MCP Agent gateway starting on %s:%s", GATEWAY_HOST, GATEWAY_PORT)
    yield
    log.info("Gateway stopping")

app = FastAPI(title="Bank Agent Gateway", lifespan=lifespan)
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])


class LoginRequest(BaseModel):
    cif: str
    lang: str = "en"

class SignupRequest(BaseModel):
    cif: str
    name: str
    mobile: str
    lang: str = "en"


class ChatRequest(BaseModel):
    session_id: str
    message: str


class ChatResponse(BaseModel):
    session_id: str
    reply: str
    lang: str


@app.post("/login")
async def login(req: LoginRequest):
    """Dev login — creates a session and returns session_id."""
    sid = str(uuid.uuid4())
    try:
        client = await BankClient.dev_login(req.cif, sid, req.lang)
        me = await client.get_me()
        s = session_store.get_or_create(sid, client._token, client._lang)
        s.customer_name = me.get("fullName")
        s.lang = req.lang.lower()[:2]
        return {"session_id": sid, "customer_name": s.customer_name,
                "lang": s.lang, "preferred_language": me.get("preferredLanguage")}
    except Exception as e:
        log.error("login failed cif=%s: %s", req.cif, e)
        raise HTTPException(status_code=401, detail=str(e))

@app.post("/signup")
async def signup(req: SignupRequest):
    """Dev signup — creates an account and returns session_id."""
    sid = str(uuid.uuid4())
    try:
        client = await BankClient.dev_signup(req.cif, req.name, req.mobile, sid, req.lang)
        me = await client.get_me()
        s = session_store.get_or_create(sid, client._token, client._lang)
        s.customer_name = me.get("fullName")
        s.lang = req.lang.lower()[:2]
        return {"session_id": sid, "customer_name": s.customer_name,
                "lang": s.lang, "preferred_language": me.get("preferredLanguage")}
    except Exception as e:
        log.error("signup failed cif=%s: %s", req.cif, e)
        raise HTTPException(status_code=400, detail=str(e))


@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(req: ChatRequest):
    """Main conversation endpoint."""
    s = session_store.get(req.session_id)
    if not s:
        raise HTTPException(status_code=404, detail="Session not found. Please log in again.")
    try:
        reply = await chat(req.message, s)
        return ChatResponse(session_id=req.session_id, reply=reply, lang=s.lang)
    except Exception as e:
        log.error("chat error session=%s: %s", req.session_id, e)
        raise HTTPException(status_code=500, detail="Agent error. Please try again.")


@app.get("/session/{session_id}")
async def get_session(session_id: str):
    s = session_store.get(session_id)
    if not s:
        raise HTTPException(status_code=404, detail="Session not found")
    return {
        "session_id": s.session_id,
        "lang": s.lang,
        "customer_name": s.customer_name,
        "has_pending": s.pending is not None,
        "pending_action": s.pending.action_type if s.pending else None,
        "history_turns": len(s.history) // 2,
    }


@app.get("/health")
async def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run("gateway:app", host=GATEWAY_HOST, port=GATEWAY_PORT, reload=True)
