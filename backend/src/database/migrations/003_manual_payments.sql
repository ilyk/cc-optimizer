-- Migration: Add manual_payments table
-- Epic 4: Free Tier - Manual Payment Tracking

CREATE TABLE IF NOT EXISTS manual_payments (
    id SERIAL PRIMARY KEY,
    user_id INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    account_id TEXT NOT NULL,
    payment_date DATE NOT NULL,
    amount_cents BIGINT NOT NULL CHECK (amount_cents > 0),
    source TEXT NOT NULL DEFAULT 'manual',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    
    -- Ensure no duplicate payments
    CONSTRAINT unique_manual_payment UNIQUE (user_id, account_id, payment_date, amount_cents)
);

-- Index for efficient queries
CREATE INDEX IF NOT EXISTS idx_manual_payments_user_account 
ON manual_payments (user_id, account_id);

CREATE INDEX IF NOT EXISTS idx_manual_payments_date 
ON manual_payments (payment_date);

-- Comments
COMMENT ON TABLE manual_payments IS 'User-recorded manual payments for credit cards';
COMMENT ON COLUMN manual_payments.amount_cents IS 'Payment amount in cents (always positive)';
COMMENT ON COLUMN manual_payments.source IS 'Payment source: manual | ach | check';

