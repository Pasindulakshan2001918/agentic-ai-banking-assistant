-- Add version column for optimistic locking to accounts table
ALTER TABLE accounts ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add version column for optimistic locking to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add version column for optimistic locking to transactions table
ALTER TABLE transactions ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;

-- Add version column for optimistic locking to cards table
ALTER TABLE cards ADD COLUMN IF NOT EXISTS version BIGINT DEFAULT 0;
