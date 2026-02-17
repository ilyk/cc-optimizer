-- Add social authentication columns to users table

-- Add columns for social authentication
ALTER TABLE users ADD COLUMN name VARCHAR(255);
ALTER TABLE users ADD COLUMN auth_provider VARCHAR(50);
ALTER TABLE users ADD COLUMN provider_user_id VARCHAR(255);

-- Create index for efficient lookups by social provider
CREATE INDEX idx_users_auth_provider ON users(auth_provider);
CREATE INDEX idx_users_provider_user_id ON users(provider_user_id);

-- Make password_hash nullable for social auth users
-- Note: SQLite doesn't support ALTER COLUMN, so we'll handle this in the application logic
