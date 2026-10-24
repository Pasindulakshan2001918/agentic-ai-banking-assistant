# Agentic AI Assistant admin console

Admin UI for the existing Spring Boot `core-api`. The administrator endpoints, authentication, and database migrations live in `agentic-ai-banking-assistant/core-api`; this folder contains only the frontend.

## Run the core API

Set administrator credentials in `agentic-ai-banking-assistant/.env` (do not commit this file):

```dotenv
ADMIN_EMAIL=admin@yourbank.local
ADMIN_PASSWORD=replace-with-a-unique-password-of-at-least-14-characters
ADMIN_NAME=Bank Administrator
```

The Compose service passes these variables into Spring Boot. From the banking-assistant folder, run:

```powershell
docker compose up -d --build db core-api
```

On startup, Flyway applies migration `V4__admin_console.sql`. The admin API is part of the existing API at `http://localhost:8081/api/v1/admin`. Set the same `ADMIN_EMAIL` and `ADMIN_PASSWORD` environment variables when starting Spring Boot outside Docker. The administrator account is not created through a public registration endpoint.

The Super Admin can register an Admin from **Administration → Administrators**. The email address is the username, the initial password is stored as a BCrypt hash, and new accounts are marked `mustChangeCredentials=true`. Only the configured bootstrap Super Admin can call the registration API. The new Admin signs in using those initial credentials, is restricted to the forced credential-change endpoint until setting a unique username and password, and can later update credentials under **Administration → System settings**. Password changes require the current password. Changing credentials revokes the Admin's other active sessions.

## Local API links

- Admin frontend: `http://localhost:5173`
- Core API base: `http://localhost:8081/api/v1`
- Admin sign-in: `POST /admin/auth/login`
- Super Admin registration: `POST /admin/administrators`
- Forced first-login credential change: `POST /admin/auth/first-login-credentials`
- Future credential update: `PATCH /admin/auth/credentials`

These localhost links are for development on the machine running Docker. To access the app from other devices or the public internet, configure a deployment hostname, HTTPS reverse proxy, and matching CORS origins; do not expose the development setup directly.

## Run the admin frontend

From this folder:

```powershell
npm run dev
```

Open `http://127.0.0.1:5173`. The frontend defaults to the same host on port `8081`, for example `http://127.0.0.1:8081/api/v1`, and Spring Boot allows the local development frontend origins. Change the API base URL after signing in under **Administration → System settings**; it is intentionally not part of the sign-in form.

Run frontend tests with `npm test`.

## Implemented admin API

- `POST /admin/auth/login` and `POST /admin/auth/logout`; expiring, revocable administrator JWT sessions, bcrypt password verification, and basic login throttling.
- `GET /admin/health` and `GET /admin/dashboard?days=7|30|90`.
- Customer, transaction, bill-payment, security signal/case, AI audit, administrator and admin-event list/detail endpoints.
- Customer registration and supported `ACTIVE`/`BLOCKED` status updates; all mutations are validated and written to the append-only administrative audit trail.
- Security investigation notes/status and CSV exports/reports.
- Customer email/mobile masking in directory/detail responses.

The core banking tables remain the source of truth. The existing schema has no pending transactions, separate payment processor, independent fraud-alert feed, or dedicated AI-trace store. The admin UI therefore reports only data available in the customer, transaction, and redacted audit tables. A `DENY`/`STEP_UP` policy event is a signal to investigate, not a fraud conclusion. Admin audit events and investigation notes are stored in the isolated PostgreSQL `admin_console` schema.

For development only: configure strong private credentials and do not expose the dev-profile API or Compose database credentials to the public internet. Production requires a dedicated least-privilege database role, HTTPS, secret management, and a persistent session/revocation strategy appropriate for deployment scale.
