# Agentic AI Banking Assistant — Full Unified System

A complete banking platform featuring:
1. **Core Banking API & Shared Database** (Spring Boot + PostgreSQL)
2. **Bank Admin Operations Console** (Vanilla JS / Node.js web panel — manual only, no agent)
3. **Customer Mobile Application** (Flutter / Dart — real login, real account balance, full banking features)
4. **Agentic AI Banking Assistant** (Python MCP Agent — integrated into mobile app with real tool execution)

---

## 🏗️ Architecture & Unified Database

All components connect to the **same single PostgreSQL database** (`bank`):

```
                        ┌──────────────────────────────────────┐
                        │    PostgreSQL Database (port 5432)   │
                        │ customers · accounts · transactions  │
                        │ user_credentials · admin_accounts    │
                        └──────────────────┬───────────────────┘
                                           │
                                           │ (JDBC)
                                           ▼
                        ┌──────────────────────────────────────┐
                        │      Spring Boot Core API (8081)     │
                        │   /api/v1/auth/*   /api/v1/admin/*   │
                        └──────▲────────────────────────▲──────┘
                               │                        │
             Admin JWT         │                        │ Customer JWT
        ┌──────────────────────┴───────┐        ┌───────┴──────────────────────┐
        │                              │        │                              │
┌───────┴────────────────────────┐     │  ┌─────┴────────────────────────┐     │
│   Bank Admin Operations Panel  │     │  │     Customer Mobile App      │     │
│       (Web: port 5173)         │     │  │       (Flutter / Dart)       │     │
│  • admin@bank.com / Admin@1234 │     │  │  • Register with Account No  │     │
│  • Create accounts + set balance│    │  │  • Sign in with password     │     │
│  • Manual only (no agent)      │     │  │  • Live balance & transfers  │     │
└────────────────────────────────┘     │  │  • AI Agent Chat button      │     │
                                       │  └──────────────┬───────────────┘     │
                                       │                 │ (Agent Session)     │
                                       │                 ▼                     │
                                       │  ┌──────────────────────────────┐     │
                                       │  │   Python MCP Agent Gateway   │     │
                                       │  │         (port 8290)          │     │
                                       └──┤  • Executes MCP tools        ├─────┘
                                          │  • Uses Customer JWT         │
                                          └──────────────────────────────┘
```

---

## 🔑 Credentials & Flow

### 1. Bank Admin Panel
- **URL**: `http://127.0.0.1:5173`
- **Default Super Admin**:
  - Email: `admin@bank.com`
  - Password: `Admin@1234`
- **Role**: Bank staff manual operations (no AI agent).
- **Features**:
  - Live Dashboard (Total deposits, active accounts, registered users)
  - Customer Accounts list & detail view
  - **Create Customer Account**:
    - Full Name, Mobile, Email, Preferred Language
    - Account Type (`SAVINGS`, `CURRENT`, `FIXED_DEPOSIT`)
    - **Initial Balance (LKR)**: Real opening balance entered by admin (no dummy numbers)
    - Custom or auto-generated Account Number (e.g. `ACC1713980000`)
  - Transaction Audit & AI Agent Audit Logs

### 2. Customer Mobile App
- **Self Registration**:
  - Customer enters:
    1. **Account Number** (issued by the bank / created by admin)
    2. **Username** (chosen by customer)
    3. **Password** (minimum 8 characters)
  - Backend verifies account number in database and links `user_credentials`.
- **Sign In**:
  - Customer logs in with `Username` + `Password`.
  - Sees their **real account balance** entered by the bank admin.
- **AI Banking Assistant**:
  - Floating glowing **"AI Agent"** button in navigation & on home screen.
  - Chat with agent in natural language (English / Sinhala / Tamil).
  - Agent can check balance, view transactions, transfer funds, manage payees, pay bills, toggle cards with real data.

---

## 🚀 One-Command Quick Start

To launch the complete system natively:

```powershell
.\start-all.ps1
```

This starts:
- PostgreSQL database
- Spring Boot core API on `http://localhost:8081`
- Python MCP Agent Gateway on `http://localhost:8290`
- Admin Panel on `http://127.0.0.1:5173` (and opens in browser)

### Launching the Mobile App

Run the launcher script from the root directory:

```powershell
.\run-mobile.ps1
```

Or target a specific device:
- **Chrome browser (with mobile mockup frame)**: `.\run-mobile.ps1 -Device chrome`
- **Native Windows Desktop**: `.\run-mobile.ps1 -Device windows`
- **Edge browser**: `.\run-mobile.ps1 -Device edge`

---

## 🧪 Testing the Complete Real-World Flow

1. Open Admin Panel at `http://127.0.0.1:5173`.
2. Log in with `admin@bank.com` / `Admin@1234`.
3. Click **"+ New Account"**.
4. Fill in:
   - Full Name: `Janith Fernando`
   - Mobile: `0771234567`
   - Account Type: `Savings Account`
   - Initial Balance: `50000.00`
   - Account Number: `ACC555001`
5. Click **"Create Account"**. The admin panel confirms account creation.
6. Open the Mobile App:
   - Tap **"Sign Up"**.
   - Account Number: `ACC555001`
   - Username: `janith`
   - Password: `Password@123`
   - Tap **"Create Account"**.
7. Mobile app immediately logs in and displays **LKR 50,000.00** total balance!
8. Tap **"AI Agent"** button:
   - Ask: *"What is my account balance?"*
   - Agent replies with real-time balance of `ACC555001`: **LKR 50,000.00**!
