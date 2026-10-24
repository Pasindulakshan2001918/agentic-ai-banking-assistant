-- =====================================================================
-- V4: Real authentication tables + admin seed
-- user_credentials  – stores hashed passwords for registered customers
-- admin_accounts    – bank staff who log into the admin panel
-- =====================================================================

-- BCrypt-hashed password storage for registered mobile users.
-- A customer row is created by admin; user_credentials row is created when
-- the customer self-registers on the mobile app.
CREATE TABLE user_credentials (
    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id   uuid NOT NULL UNIQUE REFERENCES customers(id),
    account_no    varchar(20) NOT NULL UNIQUE,  -- account number used as identifier
    username      varchar(80) NOT NULL UNIQUE,  -- chosen by user during signup
    password_hash text NOT NULL,               -- BCrypt hash (never stored plain)
    created_at    timestamptz NOT NULL DEFAULT now(),
    last_login_at timestamptz
);
CREATE INDEX idx_user_cred_username ON user_credentials(lower(username));
CREATE INDEX idx_user_cred_account  ON user_credentials(account_no);

-- Admin staff accounts (no agent, manual panel access only).
CREATE TABLE admin_accounts (
    id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email                   varchar(254) NOT NULL UNIQUE,
    full_name               text NOT NULL,
    password_hash           text NOT NULL,   -- BCrypt hash
    role                    text NOT NULL DEFAULT 'ADMIN'
                            CHECK (role IN ('SUPER_ADMIN','ADMIN','VIEWER')),
    permissions             text[] NOT NULL DEFAULT '{*}',
    capabilities            text[] NOT NULL DEFAULT '{}',
    status                  text NOT NULL DEFAULT 'ACTIVE'
                            CHECK (status IN ('ACTIVE','DISABLED')),
    must_change_credentials boolean NOT NULL DEFAULT false,
    created_at              timestamptz NOT NULL DEFAULT now(),
    last_login_at           timestamptz
);

-- NOTE: The SUPER_ADMIN row is inserted at application startup by
-- AdminSeedService.java (which uses Spring's BCryptPasswordEncoder)
-- so the hash is correct for the configured password.
-- Default credentials: admin@bank.com / Admin@1234
