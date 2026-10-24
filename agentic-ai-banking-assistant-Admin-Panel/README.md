# 🏦 Agentic AI Banking Assistant

> A multilingual conversational AI agent for retail digital banking — built with LLaMA 3.3, LangChain, Spring Boot, and React Native.

[![Python](https://img.shields.io/badge/Python-3.10-3776AB?style=flat&logo=python&logoColor=white)](https://python.org)
[![Java](https://img.shields.io/badge/Java-17-ED8B00?style=flat&logo=openjdk&logoColor=white)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![LangChain](https://img.shields.io/badge/LangChain-0.4-1C3C3C?style=flat&logo=langchain&logoColor=white)](https://langchain.com)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.135-009688?style=flat&logo=fastapi&logoColor=white)](https://fastapi.tiangolo.com)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat&logo=postgresql&logoColor=white)](https://postgresql.org)
[![License](https://img.shields.io/badge/License-MIT-green?style=flat)](LICENSE)

---

## 📋 Table of Contents

- [Overview](#overview)
- [System Architecture](#system-architecture)
- [Features](#features)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Getting Started](#getting-started)
- [AI Agent — How It Works](#ai-agent--how-it-works)
- [API Reference](#api-reference)
- [Conversation Examples](#conversation-examples)
- [Environment Variables](#environment-variables)
- [Team](#team)

---

## Overview

This project is an **agentic AI assistant** built for a Sri Lankan retail digital banking application. Customers interact with the bank through natural conversation in **English, Sinhala, or Tamil**. The AI handles real banking tasks — checking balances, transferring money, paying bills, managing cards, and generating spending insights — by detecting intent, extracting entities, and calling the correct backend APIs automatically.

The system was built as a university group project for **EC5406 Software Group Project**, University of Ruhuna, Faculty of Engineering.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                         MOBILE APP                              │
│                  React Native  (Port 3000)                      │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │  Auth Screen │    │  AI Chat     │    │  Banking Screens │  │
│  │  (Login/Reg) │    │  Screen      │    │  (Balance/Txns)  │  │
│  └──────┬───────┘    └──────┬───────┘    └────────┬─────────┘  │
└─────────┼───────────────────┼─────────────────────┼────────────┘
          │                   │                      │
          │ POST /api/auth/*  │ POST /chat           │ GET /api/*
          │                   │                      │
          ▼                   ▼                      │
┌─────────────────┐  ┌────────────────────┐         │
│  SPRING BOOT    │◄─┤   AI AGENT         │         │
│  BACKEND        │  │   Python / FastAPI  │         │
│  Port 8081      │  │   Port 8001        │         │
│                 │  │                    │         │
│  • Auth/JWT     │  │  • detect_language │         │
│  • Accounts     │  │  • detect_intent   │         │
│  • Transfers    │  │  • route_to_tool   │         │
│  • Bills        │  │  • generate_reply  │         │
│  • Cards        │  │  • STT / TTS       │         │
│  • Insights     │  └────────────────────┘         │
│                 │◄────────────────────────────────┘
│  PostgreSQL     │
│  (agenticdb)    │
└─────────────────┘
```

### Request Flow

```
Customer types: "Send 5000 to Nimal"

Mobile App  ──POST /chat──►  AI Agent
                                │
                         detect_language()     → "en"
                         detect_intent()       → TRANSFER_MONEY
                                                  amount=5000
                                                  recipient="Nimal"
                         transfer_money_tool() → POST /api/ai/transfer
                                │
                         Spring Boot generates OTP → SMS
                                │
                         generate_response()   → "Confirm transfer of
                                                  LKR 5,000 to Nimal?"
                                │
Mobile App  ◄─────────── reply text ──────────────────────────────
```

---

## Features

### 🤖 AI Capabilities
- **Natural language understanding** in English, Sinhala, and Tamil
- **Intent detection** across 7 banking intents with 100% test accuracy
- **Entity extraction** — amounts, recipient names, bill providers, periods
- **Clarification handling** — asks follow-up questions when information is missing
- **Multi-turn conversation** — manages OTP flows across 3 message turns
- **Spending analysis** — compares monthly spending and flags anomalies > 20%
- **Speech-to-Text** using OpenAI Whisper (local, no API key)
- **Text-to-Speech** in Sinhala, Tamil, and English using gTTS

### 🏦 Banking Operations
| Feature | Description |
|---|---|
| Check Balance | Returns current account balance |
| Transaction History | Shows last N transactions with DR/CR |
| Account Summary | Balance + recent transactions in one response |
| Money Transfer | Full 3-step flow: confirm → OTP → execute |
| Bill Payment | Electricity (CEB/LECO), Water (NWS), Mobile recharge |
| Card Management | Immediate block; confirmed unblock |
| Spending Insights | Monthly category breakdown |
| Budget Warning | Month-over-month comparison with threshold alerts |

### 🌐 Multilingual Support
| Language | Script detection | Romanised detection | Translation fallback |
|---|---|---|---|
| English | — | — | Native |
| Sinhala | ✅ U+0D80–0DFF | ✅ karanna, ona, mama... | ✅ deep-translator |
| Tamil | ✅ U+0B80–0BFF | ✅ pannanum, enakku... | ✅ deep-translator |

---

## Tech Stack

### AI Agent (Python)
| Package | Version | Purpose |
|---|---|---|
| LangChain | 1.2.15 | LLM orchestration, prompt management |
| LangChain-HuggingFace | latest | HuggingFace endpoint wrapper |
| FastAPI | 0.135.3 | REST API server for AI service |
| openai-whisper | 20250625 | Local Speech-to-Text |
| gTTS | 2.5.4 | Text-to-Speech (Si/Ta/EN) |
| langdetect | latest | Language identification fallback |
| deep-translator | 1.9.1 | Si/Ta ↔ EN translation |
| httpx | 0.28.1 | HTTP client for Spring Boot calls |
| pydantic-settings | 2.13.1 | Configuration management |
| python-jose | 3.5.0 | JWT handling |
| uvicorn | 0.44.0 | ASGI server |

### Backend (Java)
| Technology | Version | Purpose |
|---|---|---|
| Spring Boot | 3.x | REST API framework |
| Spring Security | 6.x | JWT authentication |
| Spring Data JPA | 3.x | Database ORM |
| PostgreSQL | 16 | Primary database |
| Hibernate | 6.x | ORM implementation |

### Mobile App (React Native)
- React Native
- JWT-based authentication
- Chat interface with multi-turn support

---

## Project Structure

```
banking_ai_agent/
│
├── agent/
│   ├── __init__.py
│   ├── intent_detector.py       # detect_intent() — LLaMA 3.3 intent classification
│   ├── banking_agent.py         # BankingAgent class + ConversationMemory
│   ├── tools/
│   ├── prompts/
│   └── memory/
│
├── services/
│   ├── __init__.py
│   ├── banking_tools.py         # All 8 tools + route_to_tool() dispatcher
│   ├── language/
│   │   ├── __init__.py
│   │   ├── detector.py          # 3-stage language detection
│   │   └── translator.py        # Si/Ta ↔ EN translation
│   └── speech/
│       ├── __init__.py
│       ├── stt.py               # Whisper Speech-to-Text
│       └── tts.py               # gTTS Text-to-Speech
│
├── api/
│   └── __init__.py              # FastAPI app (Notebook 04)
│
├── config/
│   ├── __init__.py
│   └── settings.py              # Pydantic settings from .env
│
├── models/
│   └── __init__.py
│
├── notebooks/
│   ├── Notebook_00_Environment.ipynb
│   ├── Notebook_01_Intent_Detection.ipynb
│   ├── Notebook_02_Banking_Tools.ipynb
│   └── Notebook_03_Agent.ipynb
│
├── .env                         # Environment variables (not committed)
├── .env.example                 # Template for .env
├── requirements.txt
└── README.md
```

---

## Getting Started

### Prerequisites

- Python 3.10+
- Anaconda / Miniconda
- Java 17+
- PostgreSQL 16
- Node.js 18+ (for mobile app)
- A [Hugging Face](https://huggingface.co) account with API key

---

### 1. Database Setup

```sql
-- Create the database
CREATE DATABASE agenticdb;
CREATE USER admin WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE agenticdb TO admin;
```

---

### 2. Spring Boot Backend

```bash
# Clone and navigate to backend
cd banking-backend

# Configure application.properties
spring.datasource.url=jdbc:postgresql://localhost:5432/agenticdb
spring.datasource.username=admin
spring.datasource.password=yourpassword
spring.jpa.hibernate.ddl-auto=update

# Run
./mvnw spring-boot:run
# Backend starts on http://localhost:8081

# Verify
curl http://localhost:8081/api/public/health
# Expected: { "status": "UP", "message": "Banking system is operational" }
```

---

### 3. AI Agent Setup

```bash
# Create and activate Conda environment
conda create -n banking-ai python=3.10
conda activate banking-ai

# Install all packages
pip install langchain langchain-huggingface langchain-core langchain-community
pip install huggingface-hub fastapi pydantic pydantic-settings
pip install httpx python-dotenv python-jose uvicorn requests
pip install openai-whisper langdetect deep-translator gtts soundfile numpy
```

---

### 4. Environment Variables

```bash
# Create .env file in project root
cp .env.example .env
# Edit .env with your values (see Environment Variables section below)
```

---

### 5. Register a Test User

```bash
# Run the test registration script
python test_register.py
# Creates: testcustomer / Test@1234
```

---

### 6. Run the Notebooks

Open Jupyter and run in order:

```
Notebook_00 → Environment verification + multilingual services
Notebook_01 → Intent detection testing
Notebook_02 → Banking tools + backend integration testing
Notebook_03 → Full agent simulation
```

---

### 7. Start the AI Agent API

```bash
uvicorn api.main:app --host 0.0.0.0 --port 8001 --reload
# AI Agent starts on http://localhost:8001
```

---

## AI Agent — How It Works

### The 6-Step Pipeline

Every customer message goes through exactly these steps inside `BankingAgent.chat()`:

```
Step 1  detect_language()
        Unicode check → Romanised keywords → langdetect
        Returns: "si" | "ta" | "en"

Step 2  translate_to_english()  [only if Si/Ta]
        Translates message to English for intent detection
        Original language preserved for the reply

Step 3  detect_intent() OR _handle_flow_continuation()
        If mid-flow (OTP/confirmation) → parse yes/no or 6-digit OTP
        Otherwise → call LLaMA 3.3 → returns JSON with intent + entities

Step 4  route_to_tool()
        Maps intent → correct tool function → calls Spring Boot API

Step 5  _update_flow_state()
        Updates ConversationMemory:
          SUCCESS/CANCELLED → clear_flow()
          AWAITING_CONFIRMATION/OTP → save step + entities

Step 6  generate_response()
        LLaMA 3.3 turns raw tool result into natural language
        Reply generated in customer's detected language
```

---

### The 7 Intents

| Intent | Example message | Entities extracted |
|---|---|---|
| `CHECK_BALANCE` | "What's my balance?" | account_type |
| `TRANSFER_MONEY` | "Send 5000 to Nimal" | amount, recipient_name, account_number |
| `PAY_BILL` | "Pay my CEB bill" | bill_type, provider_name, amount |
| `CARD_ACTION` | "Block my debit card" | action, card_type |
| `GET_INSIGHTS` | "How much did I spend?" | period, category |
| `TRANSACTION_HISTORY` | "Show last 5 transactions" | count, period |
| `UNKNOWN` | "What's the weather?" | — |

---

### Multi-Turn OTP Flow

```
Turn 1  Customer: "Send 5000 to Nimal"
        Agent:    "Confirm transfer of LKR 5,000 to Nimal?"
        State:    AWAITING_CONFIRMATION

Turn 2  Customer: "Yes"
        Agent:    "OTP sent to your phone. Please enter the 6-digit code."
        State:    AWAITING_OTP
        [Spring Boot sends SMS]

Turn 3  Customer: "482193"
        Agent:    "Transfer of LKR 5,000 to Nimal completed successfully!"
        State:    cleared
```

---

### ConversationMemory

```python
memory.customer_token     # JWT — used for every API call
memory.detected_language  # "si" | "ta" | "en"
memory.current_intent     # e.g. "TRANSFER_MONEY"
memory.flow_state         # { step, amount, recipient, ... }
memory.history            # last 20 message turns
memory.is_in_flow()       # True when mid-OTP or mid-confirmation
```

---

## API Reference

### AI Agent Endpoints  `localhost:8001`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/chat` | Send a text message, receive a reply |
| `POST` | `/voice` | Upload audio, receive a text + audio reply |
| `GET` | `/health` | Service health check |

**POST /chat — Request**
```json
{
  "message": "Send 5000 to Nimal",
  "language": "en"
}
```
**Header:** `Authorization: Bearer <JWT>`

**POST /chat — Response**
```json
{
  "reply": "Confirm transfer of LKR 5,000 to Nimal? Type yes to confirm.",
  "language": "en",
  "intent": "TRANSFER_MONEY",
  "requires_clarification": false
}
```

---

### Spring Boot Endpoints  `localhost:8081`

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new user |
| `POST` | `/api/auth/login` | Login, returns JWT |
| `GET` | `/api/ai/balance` | Account balance |
| `GET` | `/api/ai/transactions?limit=N` | Recent transactions |
| `GET` | `/api/ai/insights?year=Y&month=M` | Spending insights |
| `POST` | `/api/ai/transfer` | Money transfer (3-step) |
| `POST` | `/api/ai/bill/pay` | Bill payment (3-step) |
| `POST` | `/api/ai/card/block` | Block card immediately |
| `POST` | `/api/ai/card/unblock` | Unblock card |
| `GET` | `/api/public/health` | Backend health check |

All `/api/ai/*` endpoints require `Authorization: Bearer <JWT>` header.

---

## Conversation Examples

### English
```
Customer : What is my account balance?
Savi     : Your current balance is LKR 50,000.

Customer : Show my last 3 transactions
Savi     : Here are your last 3 transactions:
           - Salary Credit: LKR 85,000 (CR)
           - CEB Payment: LKR 4,200 (DR)
           - Supermarket: LKR 3,150 (DR)
```

### Sinhala Script
```
Customer : ගිණුමේ ශේෂය කීයද
Savi     : ඔබගේ ගිණුමේ ශේෂය රු. 50,000 කි.
```

### Sinhala Romanised (Code-switched)
```
Customer : CEB bill pay karanna
Savi     : CEB ELECTRICITY bill — ගෙවිය යුතු මුදල කීයද?

Customer : 4200
Savi     : CEB ELECTRICITY — රු. 4,200 ගෙවන්නද?
           ඔව් (yes) හෝ නෑ (no) ලෙස පිළිතුරු දෙන්න.
```

### Tamil Script
```
Customer : என் கார்டை block செய்யுங்கள்
Savi     : உங்கள் அட்டை உடனடியாக தடுக்கப்பட்டது.
```

### Spending Insights
```
Customer : How much did I spend this month?
Savi     : You spent LKR 28,500 this month.
           Breakdown — Food: LKR 12,000 | Utilities: LKR 8,200 |
           Transport: LKR 4,500 | Shopping: LKR 3,800

           ⚠ Your food spending is 41% higher than last month
             (LKR 12,000 vs LKR 8,500).
```

---

## Environment Variables

Create a `.env` file in the project root:

```env
# Hugging Face
HF_API_KEY=hf_your_key_here
HF_MODEL_NAME=meta-llama/Llama-3.3-70B-Instruct
HF_TEMPERATURE=0.1
HF_MAX_NEW_TOKENS=2048

# Spring Boot Backend
SPRING_BOOT_BASE_URL=http://localhost:8081
INTERNAL_API_KEY=your-internal-api-key

# JWT
JWT_SECRET=your-base64-encoded-secret
JWT_ALGORITHM=HS256

# FastAPI AI Service
AI_SERVICE_PORT=8001
AGENT_VERBOSE=true

# Multilingual
SUPPORTED_LANGUAGES=si,ta,en
DEFAULT_LANGUAGE=en
WHISPER_MODEL_SIZE=base
USE_TRANSLATION_FALLBACK=true
TRANSLATION_FALLBACK_THRESHOLD=0.5
TTS_PROVIDER=gtts
TTS_SLOW_MODE=false
```

| Variable | Options | Description |
|---|---|---|
| `HF_API_KEY` | `hf_...` | Hugging Face API key — get from hf.co/settings/tokens |
| `WHISPER_MODEL_SIZE` | `tiny` `base` `small` `medium` `large` | Larger = more accurate, slower. Use `base` for dev |
| `TTS_SLOW_MODE` | `true` `false` | Slower pronunciation — recommended for Si/Ta |
| `USE_TRANSLATION_FALLBACK` | `true` `false` | Translate Si/Ta to EN before intent detection |

---

## Team

| Role | Responsibility |
|---|---|
| AI Agent Developer | Python AI layer — intent detection, tools, agent, multilingual support (Notebooks 00–03) |
| Backend Developer | Spring Boot REST API, database, JWT security, OTP service |
| Mobile Developer | React Native app, chat interface, banking screens |

**Course:** EC5406 Software Group Project
**Department:** Electrical and Information Engineering
**Faculty:** Engineering, University of Ruhuna
**Sprint:** 4

---

## Acknowledgements

- [LLaMA 3.3-70B-Instruct](https://huggingface.co/meta-llama/Llama-3.3-70B-Instruct) by Meta AI
- [LangChain](https://langchain.com) for LLM orchestration
- [OpenAI Whisper](https://github.com/openai/whisper) for multilingual Speech-to-Text
- [gTTS](https://gtts.readthedocs.io) for Text-to-Speech

---

<p align="center">Built for EC5406 Software Group Project — University of Ruhuna</p>
