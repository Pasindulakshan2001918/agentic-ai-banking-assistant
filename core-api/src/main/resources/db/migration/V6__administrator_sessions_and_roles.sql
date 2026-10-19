ALTER TABLE admin_console.admin_sessions
    ADD COLUMN role text NOT NULL DEFAULT 'SUPER_ADMIN'
        CHECK (role IN ('SUPER_ADMIN', 'ADMIN')),
    ADD COLUMN administrator_id uuid
        REFERENCES admin_console.administrators(id) ON DELETE CASCADE;

CREATE INDEX admin_sessions_administrator_idx
    ON admin_console.admin_sessions (administrator_id)
    WHERE administrator_id IS NOT NULL;
