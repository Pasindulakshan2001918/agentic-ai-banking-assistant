CREATE TABLE admin_console.administrators (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    email                    text NOT NULL,
    full_name                text NOT NULL,
    password_hash            text NOT NULL,
    role                     text NOT NULL DEFAULT 'ADMIN' CHECK (role IN ('ADMIN', 'SUPER_ADMIN')),
    must_change_credentials boolean NOT NULL DEFAULT true,
    status                   text NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'DISABLED')),
    created_by               text NOT NULL,
    created_at               timestamptz NOT NULL DEFAULT now(),
    last_login_at            timestamptz
);

CREATE UNIQUE INDEX administrators_email_unique ON admin_console.administrators (lower(email));
CREATE INDEX administrators_status_role_idx ON admin_console.administrators (status, role);
