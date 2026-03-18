-- ============================================
-- AGENTIC BANKING SYSTEM - TEST DATA SEED SCRIPT
-- ============================================
-- Run this script after database initialization to populate test data
-- Database: agenticdb (PostgreSQL)
-- Run as: psql -U admin -d agenticdb -f seed_data.sql

BEGIN;

-- ============================================
-- 1. CREATE TEST USERS
-- ============================================

INSERT INTO users (username, email, password, full_name, role, status, created_at, updated_at, created_by)
VALUES
    -- SUPER_ADMIN
    ('super_admin', 'superadmin@agentic.com', '$2a$10$password_super_admin', 'Super Administrator', 'SUPER_ADMIN', 'ACTIVE', NOW(), NOW(), 'system'),
    
    -- ADMIN
    ('admin_user', 'admin@agentic.com', '$2a$10$password_admin', 'Admin User', 'ADMIN', 'ACTIVE', NOW(), NOW(), 'system'),
    
    -- CREATOR users (Makers)
    ('creator_john', 'john.creator@agentic.com', '$2a$10$password_creator1', 'John Creator', 'CREATOR', 'ACTIVE', NOW(), NOW(), 'system'),
    ('creator_jane', 'jane.creator@agentic.com', '$2a$10$password_creator2', 'Jane Creator', 'CREATOR', 'ACTIVE', NOW(), NOW(), 'system'),
    ('creator_bob', 'bob.creator@agentic.com', '$2a$10$password_creator3', 'Bob Creator', 'CREATOR', 'INACTIVE', NOW(), NOW(), 'system'),
    
    -- APPROVER users (Checkers)
    ('approver_alice', 'alice.approver@agentic.com', '$2a$10$password_approver1', 'Alice Approver', 'APPROVER', 'ACTIVE', NOW(), NOW(), 'system'),
    ('approver_mike', 'mike.approver@agentic.com', '$2a$10$password_approver2', 'Mike Approver', 'APPROVER', 'ACTIVE', NOW(), NOW(), 'system'),
    
    -- VIEWER users (Auditors)
    ('auditor_sarah', 'sarah.auditor@agentic.com', '$2a$10$password_viewer1', 'Sarah Auditor', 'VIEWER', 'ACTIVE', NOW(), NOW(), 'system'),
    ('compliance_officer', 'compliance@agentic.com', '$2a$10$password_viewer2', 'Compliance Officer', 'VIEWER', 'ACTIVE', NOW(), NOW(), 'system');

-- ============================================
-- 2. CREATE TEST ACCOUNTS
-- ============================================

INSERT INTO accounts (account_number, user_id, account_type, balance, interest_rate, status, currency, created_at, updated_at, created_by)
VALUES
    -- Creator John's accounts
    ('ACC-CREATOR-JOHN-001', 3, 'SAVINGS', 50000.00, 3.50, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    ('ACC-CREATOR-JOHN-002', 3, 'CHECKING', 25000.00, 0.00, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    
    -- Creator Jane's accounts
    ('ACC-CREATOR-JANE-001', 4, 'SAVINGS', 75000.00, 3.50, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    ('ACC-CREATOR-JANE-002', 4, 'CHECKING', 40000.00, 0.00, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    
    -- Creator Bob's account (inactive user)
    ('ACC-CREATOR-BOB-001', 5, 'SAVINGS', 30000.00, 3.50, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    
    -- Approver Alice's accounts
    ('ACC-APPROVER-ALICE-001', 6, 'SAVINGS', 100000.00, 3.50, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    ('ACC-APPROVER-ALICE-002', 6, 'MONEY_MARKET', 150000.00, 4.25, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    
    -- Corporate account for testing
    ('ACC-CORPORATE-001', 1, 'CHECKING', 500000.00, 0.00, 'ACTIVE', 'USD', NOW(), NOW(), 'admin'),
    ('ACC-CORPORATE-002', 1, 'SAVINGS', 1000000.00, 3.50, 'ACTIVE', 'USD', NOW(), NOW(), 'admin');

-- ============================================
-- 3. CREATE TEST TRANSACTIONS (PENDING)
-- ============================================

INSERT INTO transactions (from_account_id, to_account_id, amount, type, status, description, reference_number, created_at, created_by, is_auto_approved)
VALUES
    -- Transaction 1: PENDING - awaiting approval
    (1, 2, 5000.00, 'TRANSFER', 'PENDING', 'Payment for invoice #2601', 'TXN-A1B2C3D4', NOW(), 'creator_john', FALSE),
    
    -- Transaction 2: PENDING - awaiting approval
    (3, 4, 10000.00, 'TRANSFER', 'PENDING', 'Monthly business expense', 'TXN-E5F6G7H8', NOW(), 'creator_jane', FALSE),
    
    -- Transaction 3: PENDING - awaiting approval
    (5, 7, 3500.00, 'TRANSFER', 'PENDING', 'Vendor payment', 'TXN-I9J0K1L2', NOW(), 'creator_bob', FALSE);

-- ============================================
-- 4. CREATE TEST TRANSACTIONS (APPROVED)
-- ============================================

INSERT INTO transactions (from_account_id, to_account_id, amount, type, status, description, reference_number, created_at, created_by, approved_at, approved_by, is_auto_approved)
VALUES
    -- Transaction 4: APPROVED
    (1, 2, 2500.00, 'TRANSFER', 'APPROVED', 'Client reimbursement', 'TXN-M3N4O5P6', NOW() - INTERVAL '2 days', 'creator_john', NOW() - INTERVAL '1 day', 'approver_alice', FALSE),
    
    -- Transaction 5: APPROVED
    (3, 4, 7500.00, 'TRANSFER', 'APPROVED', 'Salary payment', 'TXN-Q7R8S9T0', NOW() - INTERVAL '3 days', 'creator_jane', NOW() - INTERVAL '2 days', 'approver_mike', FALSE),
    
    -- Transaction 6: APPROVED (large transaction)
    (8, 9, 100000.00, 'TRANSFER', 'APPROVED', 'Fund transfer to reserve', 'TXN-U1V2W3X4', NOW() - INTERVAL '5 days', 'creator_john', NOW() - INTERVAL '4 days', 'approver_alice', FALSE);

-- ============================================
-- 5. CREATE TEST TRANSACTIONS (REJECTED)
-- ============================================

INSERT INTO transactions (from_account_id, to_account_id, amount, type, status, description, reference_number, created_at, created_by, rejected_at, rejected_by, rejection_reason)
VALUES
    -- Transaction 7: REJECTED
    (3, 1, 50000.00, 'TRANSFER', 'REJECTED', 'Suspicious large transfer', 'TXN-Y5Z6A7B8', NOW() - INTERVAL '4 days', 'creator_jane', NOW() - INTERVAL '3 days', 'approver_mike', 'Amount exceeds daily limit'),
    
    -- Transaction 8: REJECTED
    (5, 2, 15000.00, 'TRANSFER', 'REJECTED', 'Unusual pattern detected', 'TXN-C9D0E1F2', NOW() - INTERVAL '6 days', 'creator_bob', NOW() - INTERVAL '5 days', 'approver_alice', 'Verification failed: Account restricted');

-- ============================================
-- 6. CREATE AUDIT LOGS
-- ============================================

INSERT INTO audit_logs (entity_type, entity_id, action, performed_by, old_value, new_value, change_details, ip_address, user_agent, created_at)
VALUES
    -- User creation audits
    ('User', 3, 'CREATE', 'admin_user', NULL, '{"id":3,"username":"creator_john","role":"CREATOR","status":"ACTIVE"}', 'User created with CREATOR role', '192.168.1.100', 'Mozilla/5.0', NOW() - INTERVAL '10 days'),
    ('User', 4, 'CREATE', 'admin_user', NULL, '{"id":4,"username":"creator_jane","role":"CREATOR","status":"ACTIVE"}', 'User created with CREATOR role', '192.168.1.100', 'Mozilla/5.0', NOW() - INTERVAL '10 days'),
    
    -- Account creation audits
    ('Account', 1, 'CREATE', 'admin_user', NULL, '{"id":1,"accountNumber":"ACC-CREATOR-JOHN-001","type":"SAVINGS","balance":50000,"status":"ACTIVE"}', 'Savings account created for creator_john', '192.168.1.100', 'Mozilla/5.0', NOW() - INTERVAL '9 days'),
    
    -- Transaction creation audits
    ('Transaction', 1, 'CREATE', 'creator_john', NULL, '{"id":1,"amount":5000,"type":"TRANSFER","status":"PENDING"}', 'Transaction created and awaiting approval', '192.168.1.101', 'Mozilla/5.0', NOW()),
    ('Transaction', 2, 'CREATE', 'creator_jane', NULL, '{"id":2,"amount":10000,"type":"TRANSFER","status":"PENDING"}', 'Transaction created and awaiting approval', '192.168.1.102', 'Mozilla/5.0', NOW()),
    
    -- Transaction approval audits
    ('Transaction', 4, 'APPROVE', 'approver_alice', '{"status":"PENDING"}', '{"status":"APPROVED","approvedAt":"2026-03-17"}', 'Transaction approved and funds transferred', '192.168.1.200', 'Mozilla/5.0', NOW() - INTERVAL '1 day'),
    ('Transaction', 5, 'APPROVE', 'approver_mike', '{"status":"PENDING"}', '{"status":"APPROVED","approvedAt":"2026-03-16"}', 'Transaction approved and funds transferred', '192.168.1.201', 'Mozilla/5.0', NOW() - INTERVAL '2 days'),
    
    -- Transaction rejection audits
    ('Transaction', 7, 'REJECT', 'approver_mike', '{"status":"PENDING"}', '{"status":"REJECTED","rejectedAt":"2026-03-14"}', 'Transaction rejected - Amount exceeds daily limit', '192.168.1.201', 'Mozilla/5.0', NOW() - INTERVAL '3 days'),
    
    -- User role update audit
    ('User', 3, 'UPDATE_ROLE', 'admin_user', '{"role":"CREATOR"}', '{"role":"CREATOR"}', 'User role updated (no change)', '192.168.1.100', 'Mozilla/5.0', NOW() - INTERVAL '5 days');

-- ============================================
-- 7. VERIFY DATA
-- ============================================

-- Count of records
SELECT 'Users' as entity, COUNT(*) as count FROM users
UNION ALL
SELECT 'Accounts', COUNT(*) FROM accounts
UNION ALL
SELECT 'Transactions', COUNT(*) FROM transactions
UNION ALL
SELECT 'Audit Logs', COUNT(*) FROM audit_logs;

-- Summary of transactions by status
SELECT status, COUNT(*) as count FROM transactions GROUP BY status;

-- Account balances
SELECT account_number, user_id, balance, status FROM accounts ORDER BY balance DESC;

-- Pending transactions
SELECT id, amount, created_by, status, reference_number FROM transactions WHERE status = 'PENDING';

COMMIT;

-- ============================================
-- HELPFUL QUERIES FOR TESTING
-- ============================================

-- Get all transactions for an account
-- SELECT * FROM transactions WHERE from_account_id = 1 OR to_account_id = 1;

-- Get audit trail for a transaction
-- SELECT * FROM audit_logs WHERE entity_type = 'Transaction' AND entity_id = 1 ORDER BY created_at DESC;

-- Get all actions performed by a user
-- SELECT * FROM audit_logs WHERE performed_by = 'approver_alice' ORDER BY created_at DESC;

-- Get transactions created today
-- SELECT * FROM transactions WHERE DATE(created_at) = CURRENT_DATE;

-- Get balance summary
-- SELECT u.username, a.account_number, a.balance FROM accounts a JOIN users u ON a.user_id = u.id ORDER BY a.balance DESC;

-- Count pending transactions per approver queue
-- SELECT COUNT(*) as pending_count FROM transactions WHERE status = 'PENDING';

-- ============================================
-- NOTE: Passwords in this seed script are NOT hashed!
-- For production, use BCrypt hashing:
-- Example: $2a$10$salted_bcrypt_hash_here
-- Use: https://bcrypt-generator.com/ or Spring's BCryptPasswordEncoder
-- ============================================
