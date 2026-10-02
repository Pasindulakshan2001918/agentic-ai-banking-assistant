# bank-core (Spring Boot 3.5, Java 21)

The deterministic half of the banking assistant: rules, OTP, money, audit. The Python/MCP layer calls it over REST
(see `../API_CONTRACT.md`).

## Status (be honest about what is verified)

| Part | State |
|---|---|
| Migrations V1-V3 | Loaded on a real PostgreSQL 16. The critical SQL (biller search, rule lookup, ledger path, double-post guard) was executed. |
| `PolicyEngine` + 16 unit tests | Compiled and run: 16/16 pass. |
| Rest of the Java (controllers, services, repos, security) | Written carefully but **not compiled**. Maven Central was unreachable from my sandbox. First `mvn test` on your machine may show a typo or two. Paste me the errors and I'll fix them fast. |

Implemented endpoints: `POST /auth/dev-login`, `GET /me /accounts /payees /billers /billers/registered`,
`POST /actions/prepare`, `POST /actions/{id}/confirm`, `POST /actions/{id}/otp/resend`, `POST /actions/{id}/cancel`,
`GET /receipts/{txnRef}`, `POST /internal/audit`.
Action types: `FUND_TRANSFER`, `BILL_PAYMENT`.
Not yet: cards, loans, limits, insights, branches, FAQs, promotions (next batch).

## Run (Windows, PowerShell)

1. Start Postgres (Docker Desktop):
   ```
   docker run --name bankdb -e POSTGRES_USER=bank -e POSTGRES_PASSWORD=bank -e POSTGRES_DB=bank -p 5432:5432 -d postgres:16
   ```
2. Build and test: `mvn test`   (unit tests need no database)
3. Run: `mvn spring-boot:run`   (profile `dev` is the default: Flyway creates the schema, messages and demo data)

Config comes from env vars: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`, `OTP_PEPPER`. The `dev` profile has
throwaway values. Outside dev the app refuses to start without `JWT_SECRET` and `OTP_PEPPER`.

## Try it (use `curl.exe`, not the PowerShell alias)

```powershell
# 1. login as demo customer Nimal (CIF0001)
$r = curl.exe -s -X POST http://localhost:8080/api/v1/auth/dev-login -H "Content-Type: application/json" -d '{\"cif\":\"CIF0001\"}' | ConvertFrom-Json
$t = $r.accessToken
$h = @("-H","Authorization: Bearer $t","-H","Accept-Language: en","-H","Content-Type: application/json")

# 2. accounts
curl.exe -s http://localhost:8080/api/v1/accounts @h

# 3. bill payment with missing info -> NEEDS_INFO (asks for account, amount)
curl.exe -s -X POST http://localhost:8080/api/v1/actions/prepare @h -d '{\"actionType\":\"BILL_PAYMENT\",\"params\":{\"registeredBillerId\":\"e1111111-0000-0000-0000-000000000001\"}}'

# 4. small transfer to "Amma" -> ALLOW, then confirm with the returned id + token
#    (payee ids come from GET /payees)
```
Expected policy behaviour with the seeded demo rules (interbank rail JustPay, OTP-free limit LKR 10,000):
- Transfer 5,000 to Amma: `ALLOW`.
- Transfer 30,000 to Amma: `STEP_UP` (`OTP_HIGH_VALUE`); the OTP appears as `devOtp` in the response.
- Transfer anything to "Landlord" (added 3 hours ago): `STEP_UP` (`OTP_NEW_PAYEE`).
- Use `Accept-Language: si` or `ta` to get Sinhala/Tamil explanations.

## Adding a banking feature

1. Add a constant to `ActionType`.
2. Add a `@Component` extending `LedgerActionHandler` (or implementing `ActionHandler` for non-money actions).
3. Add rows to `product_rules` and `i18n_messages`.

The engine, OTP, confirm flow and audit need no changes.

## Known limits (deliberate for the 3-day scope)

- OTP is simulated (log + `devOtp`); `OtpSender` is the seam for a real SMS gateway.
- Messages are cached at first use; call `MessageService.refresh()` after editing `i18n_messages`.
- Receipt text is stored in the language used at prepare time.
- No integration tests yet (Testcontainers is the natural next step).
