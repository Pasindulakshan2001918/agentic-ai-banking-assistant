CREATE SCHEMA IF NOT EXISTS admin_console;

CREATE TABLE IF NOT EXISTS admin_console.admin_sessions (
    id          uuid PRIMARY KEY,
    email       text NOT NULL,
    created_at  timestamptz NOT NULL DEFAULT now(),
    expires_at  timestamptz NOT NULL
);

CREATE INDEX idx_admin_sessions_expiry ON admin_console.admin_sessions(expires_at);

CREATE TABLE IF NOT EXISTS admin_console.admin_events (
    id             uuid PRIMARY KEY,
    admin_email    text NOT NULL,
    action         text NOT NULL,
    resource_type  text NOT NULL,
    resource_id    text,
    outcome        text NOT NULL CHECK (outcome IN ('SUCCESS', 'DENIED')),
    details        jsonb NOT NULL DEFAULT '{}'::jsonb,
    created_at     timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_events_created ON admin_console.admin_events(created_at DESC);

CREATE TABLE IF NOT EXISTS admin_console.security_cases (
    alert_id    bigint PRIMARY KEY REFERENCES public.audit_log(id) ON DELETE RESTRICT,
    status      text NOT NULL CHECK (status IN ('OPEN', 'INVESTIGATING', 'RESOLVED', 'FALSE_POSITIVE')),
    notes       text NOT NULL,
    assigned_to text,
    updated_by  text NOT NULL,
    updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE OR REPLACE FUNCTION admin_console.reject_admin_event_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'admin audit events are append-only';
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS admin_events_append_only ON admin_console.admin_events;

CREATE TRIGGER admin_events_append_only
    BEFORE UPDATE OR DELETE ON admin_console.admin_events
    FOR EACH ROW EXECUTE FUNCTION admin_console.reject_admin_event_mutation();
