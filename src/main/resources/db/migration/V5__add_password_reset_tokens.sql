-- Password reset tokens
CREATE TABLE password_reset_tokens
(
    id         UUID PRIMARY KEY             DEFAULT gen_random_uuid(),
    user_id    UUID                NOT NULL REFERENCES users (id),
    token      VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP           NOT NULL,
    used       BOOLEAN             NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP           NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_password_reset_token ON password_reset_tokens (token);
