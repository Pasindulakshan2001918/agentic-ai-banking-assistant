-- =====================================================================
-- Agentic Digital Banking Assistant - PostgreSQL schema (v1)
-- Owner of this schema: Spring Boot (Flyway: V1__schema.sql).
-- Python/MCP never touches these tables directly; it goes through REST.
-- Money: numeric(18,2), LKR. Times: timestamptz. Business day: Asia/Colombo.
-- Requires PostgreSQL 13+ (gen_random_uuid) and contrib pg_trgm.
-- =====================================================================

CREATE EXTENSION IF NOT EXISTS pg_trgm;

-- ---------- Customers & accounts --------------------------------------
CREATE TABLE customers (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    cif                 varchar(20)  NOT NULL UNIQUE,
    full_name           text         NOT NULL,
    mobile              varchar(20)  NOT NULL,
    email               text,
    preferred_language  char(2)      NOT NULL DEFAULT 'EN' CHECK (preferred_language IN ('EN','SI','TA')),
    status              text         NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED')),
    created_at          timestamptz  NOT NULL DEFAULT now()
);

CREATE TABLE accounts (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   uuid         NOT NULL REFERENCES customers(id),
    account_no    varchar(20)  NOT NULL UNIQUE,
    product_type  text         NOT NULL CHECK (product_type IN ('SAVINGS','CURRENT','FIXED_DEPOSIT')),
    nickname      text,
    currency      char(3)      NOT NULL DEFAULT 'LKR',
    balance       numeric(18,2) NOT NULL DEFAULT 0 CHECK (balance >= 0),
    status        text         NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','DORMANT','CLOSED')),
    opened_at     timestamptz  NOT NULL DEFAULT now()
);
CREATE INDEX idx_accounts_customer ON accounts(customer_id);

CREATE TABLE deposit_terms (            -- only for FIXED_DEPOSIT accounts
    account_id     uuid PRIMARY KEY REFERENCES accounts(id),
    term_months    int           NOT NULL CHECK (term_months > 0),
    interest_rate  numeric(5,2)  NOT NULL,
    maturity_date  date          NOT NULL
);

CREATE TABLE cards (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id          uuid        NOT NULL REFERENCES customers(id),
    card_type            text        NOT NULL CHECK (card_type IN ('CREDIT','DEBIT')),
    network              text        NOT NULL DEFAULT 'VISA',
    card_last4           char(4)     NOT NULL,           -- never store full PAN
    nickname             text,
    credit_limit         numeric(18,2),
    outstanding_balance  numeric(18,2) NOT NULL DEFAULT 0,
    min_due              numeric(18,2) NOT NULL DEFAULT 0,
    statement_due_date   date,
    status               text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','BLOCKED','LOST')),
    online_txn_enabled   boolean     NOT NULL DEFAULT true,
    linked_account_id    uuid        REFERENCES accounts(id)   -- debit cards
);
CREATE INDEX idx_cards_customer ON cards(customer_id);

CREATE TABLE loans (                    -- loans, leases, pawning
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         uuid        NOT NULL REFERENCES customers(id),
    loan_no             varchar(20) NOT NULL UNIQUE,
    product_type        text        NOT NULL CHECK (product_type IN ('LOAN','LEASE','PAWNING')),
    principal           numeric(18,2) NOT NULL,
    outstanding         numeric(18,2) NOT NULL CHECK (outstanding >= 0),
    installment_amount  numeric(18,2),
    next_due_date       date,
    status              text        NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','SETTLED'))
);
CREATE INDEX idx_loans_customer ON loans(customer_id);

-- ---------- Payees & billers ------------------------------------------
CREATE TABLE payees (
    id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id       uuid        NOT NULL REFERENCES customers(id),
    nickname          text        NOT NULL,
    beneficiary_name  text        NOT NULL,
    bank_code         varchar(10) NOT NULL,
    bank_name         text        NOT NULL,
    account_no        varchar(30) NOT NULL,
    added_at          timestamptz NOT NULL DEFAULT now(),   -- drives new-payee cooling rule
    UNIQUE (customer_id, bank_code, account_no)
);
CREATE INDEX idx_payees_nick_trgm ON payees USING gin (nickname gin_trgm_ops);

CREATE TABLE billers (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    code             varchar(20) NOT NULL UNIQUE,
    name_en          text NOT NULL,
    name_si          text NOT NULL,
    name_ta          text NOT NULL,
    category         text NOT NULL CHECK (category IN ('ELECTRICITY','WATER','TELECOM','INSURANCE','TV','OTHER')),
    reference_label  text NOT NULL DEFAULT 'Account number',
    aliases          text[] NOT NULL DEFAULT '{}',          -- "ceb", "electricity", "light bill"
    active           boolean NOT NULL DEFAULT true
);
CREATE INDEX idx_billers_alias   ON billers USING gin (aliases);
CREATE INDEX idx_billers_en_trgm ON billers USING gin (name_en gin_trgm_ops);

CREATE TABLE registered_billers (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   uuid NOT NULL REFERENCES customers(id),
    biller_id     uuid NOT NULL REFERENCES billers(id),
    reference_no  varchar(40) NOT NULL,
    nickname      text,
    UNIQUE (customer_id, biller_id, reference_no)
);

CREATE TABLE bill_dues (                -- feeds proactive reminders
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    registered_biller_id  uuid NOT NULL REFERENCES registered_billers(id),
    amount_due            numeric(18,2) NOT NULL,
    due_date              date NOT NULL,
    status                text NOT NULL DEFAULT 'UNPAID' CHECK (status IN ('UNPAID','PAID'))
);

CREATE TABLE scheduled_payments (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id           uuid NOT NULL REFERENCES customers(id),
    registered_biller_id  uuid REFERENCES registered_billers(id),
    from_account_id       uuid NOT NULL REFERENCES accounts(id),
    amount                numeric(18,2) NOT NULL CHECK (amount > 0),
    frequency             text NOT NULL CHECK (frequency IN ('ONCE','MONTHLY')),
    next_run_date         date NOT NULL,
    status                text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE','PAUSED','DONE'))
);

-- ---------- Policy: product matrix, limits, OTP mandates ---------------
-- "Document as implementation": bank staff edit rows, no redeploy needed.
CREATE TABLE product_rules (
    id                    uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    action_type           text NOT NULL,
    source_product        text NOT NULL DEFAULT 'ANY',       -- SAVINGS / CURRENT / CREDIT_CARD / ANY
    rail                  text NOT NULL DEFAULT 'INTERNAL' CHECK (rail IN ('INTERNAL','JUSTPAY','LANKAPAY')),
    enabled               boolean NOT NULL DEFAULT true,
    min_amount            numeric(18,2) NOT NULL DEFAULT 0,
    max_per_txn           numeric(18,2),                      -- NULL = no cap
    max_daily             numeric(18,2),
    otp_threshold_per_txn numeric(18,2),                      -- amount above this => OTP
    otp_threshold_daily   numeric(18,2),                      -- cumulative today above this => OTP
    always_otp            boolean NOT NULL DEFAULT false,
    new_payee_cooling_hrs int NOT NULL DEFAULT 24,
    fee_flat              numeric(18,2) NOT NULL DEFAULT 0,
    fee_pct               numeric(6,4)  NOT NULL DEFAULT 0,
    notes                 text,
    UNIQUE (action_type, source_product, rail)
);

CREATE TABLE customer_limits (          -- per-customer overrides of max_daily
    customer_id  uuid NOT NULL REFERENCES customers(id),
    action_type  text NOT NULL,
    max_daily    numeric(18,2) NOT NULL,
    PRIMARY KEY (customer_id, action_type)
);

-- ---------- Conversation sessions -------------------------------------
CREATE TABLE chat_sessions (
    id              uuid PRIMARY KEY,                 -- generated by the Python gateway
    customer_id     uuid NOT NULL REFERENCES customers(id),
    language        char(2) NOT NULL CHECK (language IN ('EN','SI','TA')),
    channel         text NOT NULL DEFAULT 'WEB_CHAT',
    started_at      timestamptz NOT NULL DEFAULT now(),
    last_active_at  timestamptz NOT NULL DEFAULT now(),
    ended_at        timestamptz
);

-- ---------- Two-phase execution: prepare -> (OTP) -> confirm -----------
CREATE TABLE pending_actions (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id         uuid NOT NULL REFERENCES customers(id),
    session_id          uuid REFERENCES chat_sessions(id),
    action_type         text NOT NULL,
    params              jsonb NOT NULL,               -- normalised, validated params
    summary             jsonb NOT NULL,               -- what the customer confirms
    decision            text NOT NULL CHECK (decision IN ('ALLOW','STEP_UP','DENY')),
    reason_codes        text[] NOT NULL DEFAULT '{}',
    amount              numeric(18,2),
    fee                 numeric(18,2) NOT NULL DEFAULT 0,
    status              text NOT NULL DEFAULT 'PREPARED'
                        CHECK (status IN ('PREPARED','AWAITING_OTP','COMPLETED','FAILED','CANCELLED','EXPIRED','DENIED')),
    confirmation_token  varchar(64) NOT NULL UNIQUE,  -- random, single use
    idempotency_key     varchar(64) UNIQUE,
    expires_at          timestamptz NOT NULL,
    created_at          timestamptz NOT NULL DEFAULT now(),
    completed_at        timestamptz
);
CREATE INDEX idx_pending_customer ON pending_actions(customer_id, status);

CREATE TABLE otp_challenges (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    pending_action_id  uuid NOT NULL REFERENCES pending_actions(id),
    customer_id        uuid NOT NULL REFERENCES customers(id),
    code_hash          text NOT NULL,                 -- bcrypt/sha256+salt, never plain
    channel            text NOT NULL DEFAULT 'SMS',
    attempts           int  NOT NULL DEFAULT 0,
    max_attempts       int  NOT NULL DEFAULT 3,
    expires_at         timestamptz NOT NULL,
    verified_at        timestamptz,
    created_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_otp_pending ON otp_challenges(pending_action_id);

-- ---------- Ledger of executed actions --------------------------------
CREATE SEQUENCE txn_ref_seq START 100001;

CREATE TABLE transactions (
    id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    txn_ref            varchar(24) NOT NULL UNIQUE,   -- e.g. TXN-20260928-100001
    customer_id        uuid NOT NULL REFERENCES customers(id),
    pending_action_id  uuid UNIQUE REFERENCES pending_actions(id),   -- UNIQUE = no double execution
    action_type        text NOT NULL,
    from_account_id    uuid REFERENCES accounts(id),
    counterparty       jsonb NOT NULL DEFAULT '{}',   -- payee / biller / card / loan snapshot
    amount             numeric(18,2) NOT NULL CHECK (amount > 0),
    fee                numeric(18,2) NOT NULL DEFAULT 0,
    currency           char(3) NOT NULL DEFAULT 'LKR',
    status             text NOT NULL DEFAULT 'COMPLETED' CHECK (status IN ('COMPLETED','FAILED','REVERSED')),
    remarks            text,
    channel            text NOT NULL DEFAULT 'AI_ASSISTANT',
    created_at         timestamptz NOT NULL DEFAULT now()
);
CREATE INDEX idx_txn_customer_time ON transactions(customer_id, created_at DESC);

CREATE TABLE service_requests (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    ref_no        varchar(24) NOT NULL UNIQUE,
    customer_id   uuid NOT NULL REFERENCES customers(id),
    request_type  text NOT NULL CHECK (request_type IN ('CHEQUE_BOOK','CRIB_REPORT','STATEMENT','ADDRESS_CHANGE','OTHER')),
    details       jsonb NOT NULL DEFAULT '{}',
    status        text NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN','IN_PROGRESS','DONE')),
    created_at    timestamptz NOT NULL DEFAULT now()
);

-- ---------- Knowledge: branches, FAQs, promotions, i18n ----------------
CREATE TABLE branches (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    kind           text NOT NULL CHECK (kind IN ('BRANCH','ATM')),
    name_en        text NOT NULL,
    name_si        text NOT NULL,
    name_ta        text NOT NULL,
    city           text NOT NULL,
    address        text NOT NULL,
    latitude       numeric(9,6) NOT NULL,
    longitude      numeric(9,6) NOT NULL,
    opening_hours  text,
    services       text[] NOT NULL DEFAULT '{}'
);
CREATE INDEX idx_branches_city ON branches(lower(city));

CREATE TABLE faqs (
    id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    category     text NOT NULL,
    question_en  text NOT NULL,  question_si  text NOT NULL,  question_ta  text NOT NULL,
    answer_en    text NOT NULL,  answer_si    text NOT NULL,  answer_ta    text NOT NULL,
    keywords     text[] NOT NULL DEFAULT '{}',
    active       boolean NOT NULL DEFAULT true
);
CREATE INDEX idx_faq_q_trgm ON faqs USING gin (question_en gin_trgm_ops);

CREATE TABLE promotions (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    title_en      text NOT NULL, title_si text NOT NULL, title_ta text NOT NULL,
    body_en       text NOT NULL, body_si  text NOT NULL, body_ta  text NOT NULL,
    valid_from    date NOT NULL,
    valid_to      date NOT NULL,
    active        boolean NOT NULL DEFAULT true
);

CREATE TABLE i18n_messages (            -- mirrors the platform "property files"
    msg_key  varchar(80) NOT NULL,
    lang     char(2)     NOT NULL CHECK (lang IN ('EN','SI','TA')),
    text     text        NOT NULL,      -- {placeholders} filled by Java
    PRIMARY KEY (msg_key, lang)
);

-- ---------- Audit trail (append-only) ----------------------------------
CREATE TABLE audit_log (
    id                  bigserial PRIMARY KEY,
    ts                  timestamptz NOT NULL DEFAULT now(),
    correlation_id      uuid,
    session_id          uuid,
    customer_id         uuid,
    channel             text,
    language            char(2),
    stage               text NOT NULL CHECK (stage IN
                        ('INPUT','INTENT','TOOL_CALL','POLICY','OTP','CONFIRMATION','EXECUTION','RESPONSE','FALLBACK')),
    redacted_utterance  text,            -- PII-masked; raw text/audio is NOT stored
    intent              text,
    intent_confidence   numeric(4,3),
    slots               jsonb,           -- collected params, sensitive values masked
    tool_name           text,
    decision            text,            -- ALLOW / STEP_UP / DENY / NEEDS_INFO
    reason_codes        text[],
    explanation         text,
    pending_action_id   uuid,
    transaction_id      uuid,
    outcome             text,
    model_used          text,            -- primary or fallback model id
    latency_ms          int
);
CREATE INDEX idx_audit_session  ON audit_log(session_id, ts);
CREATE INDEX idx_audit_customer ON audit_log(customer_id, ts DESC);

CREATE FUNCTION audit_log_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_audit_immutable
    BEFORE UPDATE OR DELETE ON audit_log
    FOR EACH ROW EXECUTE FUNCTION audit_log_immutable();

-- ---------- Helper view: today's usage per action (Colombo day) --------
CREATE VIEW v_daily_usage AS
SELECT customer_id,
       action_type,
       COALESCE(SUM(amount), 0) AS used_today
FROM transactions
WHERE status = 'COMPLETED'
  AND (created_at AT TIME ZONE 'Asia/Colombo')::date = (now() AT TIME ZONE 'Asia/Colombo')::date
GROUP BY customer_id, action_type;
