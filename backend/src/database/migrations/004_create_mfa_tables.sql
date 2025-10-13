-- MFA Tables for FIDO2, Passkeys, and Biometric support

-- MFA Challenges (for FIDO2/WebAuthn)
CREATE TABLE IF NOT EXISTS mfa_challenges (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    challenge TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mfa_challenges_expires ON mfa_challenges(expires_at);

-- MFA Credentials (FIDO2 keys, passkeys)
CREATE TABLE IF NOT EXISTS mfa_credentials (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    credential_id TEXT NOT NULL,
    public_key TEXT NOT NULL,
    counter INTEGER DEFAULT 0,
    type TEXT NOT NULL CHECK(type IN ('fido2', 'passkey')),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used_at TIMESTAMP,
    UNIQUE(user_id, credential_id)
);

CREATE INDEX idx_mfa_credentials_user ON mfa_credentials(user_id);
CREATE INDEX idx_mfa_credentials_credential_id ON mfa_credentials(credential_id);

-- User MFA Settings
CREATE TABLE IF NOT EXISTS user_mfa_settings (
    user_id INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    biometric_enabled BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

