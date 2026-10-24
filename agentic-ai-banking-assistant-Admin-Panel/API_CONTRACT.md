# Banking Assistant: Spring Boot REST Contract (v1)

Contract between the **Spring Boot core** (rules, money, OTP, audit) and the **Python MCP layer** (language, dialogue, tools).
Rule of the system: **the LLM proposes, Spring Boot decides.** Python never computes limits, fees or OTP needs.

## 1. Conventions

| Item | Rule |
|---|---|
| Base URL | `/api/v1` |
| Auth | `Authorization: Bearer <customer JWT>`. Python forwards the customer's own token. There is no service superuser. |
| Session | `X-Session-Id: <uuid>` on every call (Java upserts `chat_sessions`). |
| Correlation | `X-Correlation-Id: <uuid>` per user turn (goes into `audit_log`). |
| Language | `Accept-Language: en \| si \| ta`. Java returns `explanation` and `message` strings in that language from `i18n_messages`. |
| Money | JSON **strings** with 2 decimals (`"10000.00"`), currency `LKR`. Python parses with `Decimal`, never float. |
| IDs | UUID strings. Customer-visible refs: `txn_ref`, `ref_no`. |
| Times | ISO-8601 with offset (`2026-09-28T14:05:00+05:30`). |
| Idempotency | `Idempotency-Key` header required on `POST /actions/{id}/confirm`. |

### Error envelope (HTTP 4xx/5xx = protocol problems only)

```json
{ "error": { "code": "UNAUTHORIZED", "message": "Token expired", "retryable": false, "details": {} } }
```
`code` is one of: `UNAUTHORIZED` 401, `FORBIDDEN` 403, `NOT_FOUND` 404, `VALIDATION_ERROR` 400 (malformed JSON/types), `ACTION_EXPIRED` 410, `ACTION_ALREADY_DONE` 409, `INTERNAL` 500.
**Business outcomes (denied, needs OTP, missing info) are NOT errors.** They return `200` with a `decision` (section 2). This keeps the agent's logic simple.

## 2. The decision model (core of the design)

Every state-changing request goes through `prepare` first. It always returns `200` with one `decision`:

| decision | Meaning | What the agent does |
|---|---|---|
| `NEEDS_INFO` | Mandatory param missing or ambiguous | Ask the customer using `missing[]` (with `options`), then call `prepare` again |
| `ALLOW` | Valid, no OTP needed | Show `summary`, ask yes/no, then `confirm` |
| `STEP_UP` | Valid, OTP required (already sent) | Show `summary` + `explanation`, ask yes + OTP, then `confirm` with `otp` |
| `DENY` | Policy blocks it | Relay `explanation`, offer alternatives |

Lifecycle: `prepare -> (NEEDS_INFO loop) -> ALLOW|STEP_UP -> confirm -> COMPLETED` (or `cancel` / expiry after 5 minutes).

### Prepare response shape (all action types)

```json
{
  "decision": "STEP_UP",
  "actionType": "FUND_TRANSFER",
  "pendingActionId": "9b1f...-uuid",
  "confirmationToken": "c7a9...64hex",
  "expiresAt": "2026-09-28T14:10:00+05:30",
  "summary": {
    "title": "Transfer LKR 30,000.00",
    "lines": [
      {"label": "From", "value": "Main savings (••••0001)"},
      {"label": "To",   "value": "Amma - K. Perera (Demo Bank ••••1111)"},
      {"label": "Amount", "value": "LKR 30,000.00"},
      {"label": "Fee", "value": "LKR 25.00"}
    ]
  },
  "amount": "30000.00",
  "fee": "25.00",
  "otpRequired": true,
  "reasonCodes": ["OTP_HIGH_VALUE"],
  "explanation": "This requires an OTP because the amount exceeds your OTP-free limit of LKR 10,000.00.",
  "missing": [],
  "devOtp": "123456"
}
```
Notes: `devOtp` exists **only** in the `dev` Spring profile. `summary.lines` are already localized. For `DENY` and `NEEDS_INFO`, `confirmationToken` is `null`.

### `NEEDS_INFO` example (response-as-instruction from your MCP notes)

```json
{
  "decision": "NEEDS_INFO",
  "actionType": "BILL_PAYMENT",
  "missing": [
    {"field": "amount", "type": "money", "prompt": "How much would you like to pay?",
     "hint": "Current due is LKR 5,000.00, due in 3 days."},
    {"field": "fromAccountId", "type": "choice", "prompt": "Which account should I pay from?",
     "options": [
       {"value": "a1111111-...-0001", "label": "Main savings (••••0001) - LKR 250,000.00"},
       {"value": "a1111111-...-0002", "label": "Current (••••0001) - LKR 85,000.00"}]}
  ],
  "reasonCodes": [], "explanation": null, "pendingActionId": null, "confirmationToken": null
}
```

## 3. Endpoint index

| # | Method & path | Purpose | Auth |
|---|---|---|---|
| 1 | `POST /auth/dev-login` | Demo login by CIF, returns JWT (**dev profile only**) | none |
| 2 | `GET /me` | Profile + preferred language | JWT |
| 3 | `GET /accounts` | Accounts with balances | JWT |
| 4 | `GET /accounts/{id}/transactions?limit=10` | Recent transactions | JWT |
| 5 | `GET /cards` | Cards (masked) with due info | JWT |
| 6 | `GET /loans` | Loans/leases/pawning with next due | JWT |
| 7 | `GET /payees?q=` | Saved payees (fuzzy by nickname/name) | JWT |
| 8 | `GET /billers?q=` | Biller search (alias + trigram) | JWT |
| 9 | `GET /billers/registered` | Customer's registered billers + current due | JWT |
| 10 | `GET /limits` | Daily limit, used today, remaining, OTP-free limit per action | JWT |
| 11 | `GET /insights/proactive` | Due bills, upcoming loan/card dues, recurring patterns, limit warnings | JWT |
| 12 | `GET /branches/nearby` | Nearest branches/ATMs (`lat,lng` or `city`) | JWT |
| 13 | `GET /faqs/search?q=` | FAQ lookup | JWT |
| 14 | `GET /promotions` | Active promotions | JWT |
| 15 | `POST /actions/prepare` | **Generic** prepare for any action type | JWT |
| 16 | `POST /actions/{id}/confirm` | Execute (OTP if required), idempotent | JWT |
| 17 | `POST /actions/{id}/otp/resend` | Re-issue OTP (max 3 per action) | JWT |
| 18 | `POST /actions/{id}/cancel` | Cancel a pending action | JWT |
| 19 | `GET /receipts/{txnRef}` | Receipt JSON (localized labels) | JWT |
| 20 | `POST /internal/audit` | Python writes interaction events | JWT + `X-Client: mcp-agent` |

**Why one generic prepare/confirm (15-18)?** Java registers one `ActionHandler` bean per `actionType` (Strategy pattern). A new banking feature means one new handler plus one new MCP tool, with no change to the engine, the OTP flow or the audit code. That is the requirement "add features without re-engineering".

## 4. Key endpoint details

### 1. `POST /auth/dev-login`
```json
// request
{ "cif": "CIF0001" }
// 200
{ "accessToken": "eyJ...", "expiresIn": 3600, "customerId": "1111...", "preferredLanguage": "SI" }
```

### 3. `GET /accounts`
```json
{ "accounts": [
  { "id": "a1111111-...-0001", "accountNo": "1000000001", "masked": "••••0001",
    "productType": "SAVINGS", "nickname": "Main savings", "currency": "LKR", "balance": "250000.00", "status": "ACTIVE" }
]}
```

### 5. `GET /cards`
```json
{ "cards": [
  { "id": "c1111111-...", "type": "CREDIT", "last4": "4417", "nickname": "Visa Gold", "status": "ACTIVE",
    "creditLimit": "300000.00", "outstanding": "45250.00", "minDue": "4525.00",
    "dueDate": "2026-10-04", "onlineEnabled": true }
]}
```

### 8. `GET /billers?q=ceb`  (alias, then trigram; returns best matches ranked)
```json
{ "billers": [ { "id": "b000...01", "code": "CEB", "name": "Ceylon Electricity Board",
                 "category": "ELECTRICITY", "referenceLabel": "Account number", "score": 0.98 } ] }
```
`name` follows `Accept-Language`. Ambiguity (score gap < 0.15) is the agent's cue to ask "which biller?".

### 10. `GET /limits`
```json
{ "limits": [
  { "actionType": "FUND_TRANSFER", "rail": "INTERNAL", "maxPerTxn": "1000000.00",
    "dailyLimit": "2000000.00", "usedToday": "40000.00", "remainingToday": "1960000.00",
    "otpFreePerTxn": "25000.00", "otpFreeDailyRemaining": "10000.00" }
]}
```

### 11. `GET /insights/proactive`
```json
{ "items": [
  { "kind": "BILL_DUE", "dueInDays": 3, "title": "Home electricity (CEB)", "amount": "5000.00",
    "suggestedAction": { "actionType": "BILL_PAYMENT", "params": { "registeredBillerId": "e111...01", "amount": "5000.00" } } },
  { "kind": "LOAN_DUE", "dueInDays": 4, "title": "Loan LN00001", "amount": "28500.00" },
  { "kind": "RECURRING_PATTERN", "title": "You paid CEB LKR 5,000.00 last month", "suggestedAction": { "actionType": "BILL_PAYMENT", "params": {} } },
  { "kind": "LIMIT_WARNING", "title": "80% of today's transfer limit used" }
]}
```
Called once at session start; the agent phrases the reminder in the customer's language.

### 12. `GET /branches/nearby?lat=6.93&lng=79.84&kind=ATM&limit=3` (or `?city=Kandy`)
```json
{ "results": [ { "kind": "ATM", "name": "Colombo Fort ATM", "address": "...", "distanceKm": 0.12,
                 "openingHours": "24 hours", "services": ["cash_withdrawal","balance"] } ] }
```

### 15. `POST /actions/prepare`
```json
{ "actionType": "FUND_TRANSFER",
  "params": { "fromAccountId": "a1111111-...-0001", "toPayeeId": "p...", "amount": "30000.00", "remarks": "rent" } }
```
Response: section 2. `params` per type are in section 5. Java re-validates everything (Pydantic on the Python side is convenience only, not security).

### 16. `POST /actions/{id}/confirm`  (headers: `Idempotency-Key`)
```json
// request
{ "confirmationToken": "c7a9...", "otp": "123456" }
```
```json
// 200 success
{ "status": "COMPLETED",
  "transaction": { "txnRef": "TXN-20260928-100001", "actionType": "FUND_TRANSFER",
                   "amount": "30000.00", "fee": "25.00", "completedAt": "2026-09-28T14:06:11+05:30" },
  "receipt": { "url": "/api/v1/receipts/TXN-20260928-100001" },
  "message": "Transfer completed." }

// 200 business failure (still 200; agent reads status)
{ "status": "OTP_INVALID", "attemptsLeft": 2, "message": "That code is not correct." }
{ "status": "OTP_EXPIRED", "message": "The code has expired.", "canResend": true }
{ "status": "OTP_LOCKED",  "message": "Too many attempts. This action was cancelled." }
{ "status": "DENIED_ON_RECHECK", "reasonCodes": ["INSUFFICIENT_FUNDS"], "message": "..." }
```
Confirm **re-runs the policy check** (balance/limits may have changed), then executes in one DB transaction: debit, ledger row, `pending_actions.status = COMPLETED`. `transactions.pending_action_id` is `UNIQUE`, so a replay can never double-post. Same `Idempotency-Key` returns the same 200 body.

### 19. `GET /receipts/{txnRef}`
```json
{ "txnRef": "TXN-20260928-100001", "title": "Payment receipt", "completedAt": "2026-09-28T14:06:11+05:30",
  "lines": [ {"label": "From", "value": "Main savings (••••0001)"}, {"label": "Biller", "value": "Ceylon Electricity Board"},
             {"label": "Reference", "value": "0123456789"}, {"label": "Amount", "value": "LKR 5,000.00"}, {"label": "Fee", "value": "LKR 0.00"} ],
  "footer": "Thank you for banking with us." }
```
Python renders a shareable PDF/PNG from this JSON (Day 3). Labels are localized by Java.

### 20. `POST /internal/audit`
```json
{ "stage": "INTENT", "redactedUtterance": "pay my ceb bill 5000", "intent": "BILL_PAYMENT",
  "intentConfidence": 0.93, "slots": {"biller": "CEB", "amount": "5000.00"},
  "toolName": "prepare_bill_payment", "decision": null, "outcome": "OK",
  "modelUsed": "gemini/gemini-3.1-flash-lite", "latencyMs": 812 }
```
Java itself writes `POLICY`, `OTP`, and `EXECUTION` rows inside its own transactions. Python writes `INPUT`, `INTENT`, `TOOL_CALL`, `RESPONSE`, `FALLBACK`. Raw audio/text is never stored.

## 5. Action types and `params`

| actionType | Required params | Optional | Notes |
|---|---|---|---|
| `FUND_TRANSFER` | `fromAccountId`, `amount`, **one of** `toOwnAccountId` / `toPayeeId` / `toNewAccount{bankCode,accountNo,name}` | `remarks` | New account is always `OTP_NEW_PAYEE`; payee added < cooling hours ago too |
| `BILL_PAYMENT` | `registeredBillerId` **or** (`billerId` + `referenceNo`), `fromAccountId`, `amount` | `saveBiller`, `schedule{frequency,startDate}` | Ad-hoc, registered and scheduled |
| `MOBILE_RELOAD` | `operatorBillerId`, `mobileNo`, `fromAccountId`, `amount` | | |
| `CREDIT_CARD_PAYMENT` | `cardId`, `fromAccountId`, `amount` **or** `amountType` = `MIN_DUE`/`FULL` | | |
| `CARD_BLOCK` / `CARD_UNBLOCK` | `cardId` | `reason` (`LOST`/`STOLEN`/`OTHER`) | Block: no OTP. Unblock: OTP |
| `CARD_ONLINE_TOGGLE` | `cardId`, `enabled` | | |
| `CARD_PIN_RESET` | `cardId` | | Always OTP |
| `LOAN_PAYMENT` | `loanId`, `fromAccountId`, `amount` **or** `amountType` = `INSTALLMENT`/`SETTLEMENT` | | Covers loans, leases, pawning settlement |
| `FD_OPEN` | `fromAccountId`, `amount`, `termMonths` | `maturityInstruction` | Always OTP |
| `SAVINGS_OPEN` | `fromAccountId`, `initialDeposit` | `nickname` | |
| `SERVICE_REQUEST` | `requestType` (`CHEQUE_BOOK`,`CRIB_REPORT`,`STATEMENT`,...) | `details{}` | |

Types not in the first build (`PAWNING_SETTLEMENT` detail, `CRIB_REPORT` fulfillment) can ship as thin handlers that only create a `service_requests` row.

## 6. Policy engine: evaluation order and reason codes

Order in Java (`PolicyEngine.evaluate`), first `DENY` wins, otherwise OTP reasons accumulate:

1. **Param completeness** -> `NEEDS_INFO`
2. **Customer/account state** (`ACCOUNT_INACTIVE`, `CUSTOMER_BLOCKED`) -> DENY
3. **Product matrix** (`product_rules.enabled` for action + source product) -> `PRODUCT_NOT_ALLOWED`
4. **Amount rules**: `BELOW_MIN_AMOUNT`, `ABOVE_TXN_LIMIT`, `ABOVE_DAILY_LIMIT` (uses `v_daily_usage` + `customer_limits`) -> DENY
5. **Funds**: `INSUFFICIENT_FUNDS` -> DENY
6. **Rail mandates**: interbank transfer picks rail `JUSTPAY`; its limits/fees apply (`RAIL_LIMIT_EXCEEDED`)
7. **Step-up**: `OTP_HIGH_VALUE` (over `otp_threshold_per_txn`), `OTP_DAILY_CUMULATIVE` (today + this > `otp_threshold_daily`), `OTP_NEW_PAYEE`, `OTP_MANDATORY_ACTION` (`always_otp`) -> STEP_UP
8. Otherwise -> ALLOW (`reasonCodes: ["OK"]`)

Each code has an `i18n_messages` row per language, with `{limit}` style placeholders. Every evaluation writes a `POLICY` audit row with `reasonCodes` and `explanation`.

## 7. Suggested Spring Boot layout

```
com.lunorlabs.bank
 |- api/        controllers, DTOs, exception advice (envelope in section 1)
 |- security/   JWT filter, dev-login (profile=dev)
 |- action/     ActionHandler (interface), FundTransferHandler, BillPaymentHandler, ...
 |- policy/     PolicyEngine, RuleRepository (product_rules), ReasonCode enum
 |- otp/        OtpService (hash, expiry, attempts), OtpSender (console/dev)
 |- ledger/     ExecutionService (@Transactional debit + txn row)
 |- audit/      AuditService (append-only)
 |- i18n/       MessageService (i18n_messages + {placeholders})
```

## 8. How Python tools map onto this (preview of Day 2)

| MCP tool | Calls |
|---|---|
| `get_accounts`, `get_cards`, `get_loans`, `get_limits` | 3, 5, 6, 10 |
| `search_billers`, `search_payees` | 8, 7 |
| `transfer_funds`, `pay_bill`, `reload_mobile`, `pay_credit_card`, `pay_loan`, `card_action`, `open_deposit`, `create_service_request` | 15 (typed wrappers over one `prepare_action`) |
| `confirm_action`, `resend_otp`, `cancel_action` | 16, 17, 18 |
| `find_branch`, `search_faq`, `get_promotions`, `get_proactive_insights` | 12, 13, 14, 11 |
| MCP resource `bank://policies/{action}` | reads `product_rules` summary (document-as-implementation) |
| MCP prompt `receipt_template` | 19 |

## 9. Assumptions to confirm

- Interbank transfers use one simulated JustPay/LankaPay adapter (fees and limits are demo values in `product_rules`).
- OTP is simulated (console log + `devOtp`). Swap `OtpSender` for a real SMS gateway later.
- Balances are debited in the same Postgres; there is no external core banking call. `ExecutionService` is the seam where the real ABS notification would plug in.
- Card PAN/CVV are never stored or handled. Only `last4`.
