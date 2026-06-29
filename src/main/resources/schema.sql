CREATE SCHEMA IF NOT EXISTS cv_assistant;

CREATE TABLE IF NOT EXISTS access_token (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    token         VARCHAR(512) NOT NULL UNIQUE,
    label         VARCHAR(255) NOT NULL,
    valid_until   TIMESTAMPTZ  NOT NULL,
    max_attempts  INT          NOT NULL DEFAULT 10,
    attempts_used INT          NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS conversation_session (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID        NOT NULL UNIQUE,
    token_id        UUID        NOT NULL REFERENCES access_token(id),
    started_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);