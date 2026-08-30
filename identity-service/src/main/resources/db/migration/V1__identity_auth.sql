CREATE TABLE identity.users (
    id uuid PRIMARY KEY,
    phone_e164 varchar(20) NOT NULL UNIQUE,
    phone_verified_at timestamptz,
    email varchar(255),
    display_name varchar(255) NOT NULL,
    avatar_key varchar(255),
    neighborhood_id uuid,
    status varchar(255) NOT NULL CHECK (status IN ('ACTIVE','SUSPENDED','DELETED')),
    created_at timestamptz NOT NULL,
    updated_at timestamptz,
    CONSTRAINT users_phone_format CHECK (phone_e164 ~ '^\+905[0-9]{9}$')
);
CREATE TABLE identity.user_roles (
    user_id uuid NOT NULL REFERENCES identity.users(id),
    role varchar(32) NOT NULL CHECK (role IN ('USER','MODERATOR','ADMIN')),
    PRIMARY KEY (user_id, role)
);
CREATE TABLE identity.trust_scores (
    user_id uuid PRIMARY KEY REFERENCES identity.users(id),
    score integer NOT NULL DEFAULT 50 CHECK (score BETWEEN 0 AND 100),
    completed_handovers integer NOT NULL DEFAULT 0 CHECK (completed_handovers >= 0),
    no_show_count integer NOT NULL DEFAULT 0 CHECK (no_show_count >= 0),
    report_count integer NOT NULL DEFAULT 0 CHECK (report_count >= 0),
    updated_at timestamptz NOT NULL
);
CREATE TABLE identity.auth_sessions (
    id uuid PRIMARY KEY,
    user_id uuid NOT NULL REFERENCES identity.users(id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    CONSTRAINT auth_sessions_expiry CHECK (expires_at > created_at)
);
CREATE INDEX auth_sessions_user_idx ON identity.auth_sessions(user_id);
CREATE TABLE identity.refresh_tokens (
    token_hash varchar(64) PRIMARY KEY,
    session_id uuid NOT NULL REFERENCES identity.auth_sessions(id),
    created_at timestamptz NOT NULL,
    expires_at timestamptz NOT NULL,
    consumed_at timestamptz,
    CONSTRAINT refresh_tokens_expiry CHECK (expires_at > created_at)
);
CREATE INDEX refresh_tokens_session_idx ON identity.refresh_tokens(session_id);
