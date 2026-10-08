-- Non-dialable demo IDs; these are not real mobile numbers and cannot receive SMS/calls.
INSERT IGNORE INTO users (name, email, phone_number, password_hash, role, kyc_status, created_at) VALUES
    ('ONKAR',  'onkar.demo@payflow.test',  '0000000001', '!DEMO_RECIPIENT_ONLY!', 'USER', 'DEMO', CURRENT_TIMESTAMP(6)),
    ('ANANDA', 'ananda.demo@payflow.test', '0000000002', '!DEMO_RECIPIENT_ONLY!', 'USER', 'DEMO', CURRENT_TIMESTAMP(6)),
    ('OM',     'om.demo@payflow.test',     '0000000003', '!DEMO_RECIPIENT_ONLY!', 'USER', 'DEMO', CURRENT_TIMESTAMP(6)),
    ('RAM',    'ram.demo@payflow.test',    '0000000004', '!DEMO_RECIPIENT_ONLY!', 'USER', 'DEMO', CURRENT_TIMESTAMP(6)),
    ('AMIT',   'amit.demo@payflow.test',   '0000000005', '!DEMO_RECIPIENT_ONLY!', 'USER', 'DEMO', CURRENT_TIMESTAMP(6));

INSERT IGNORE INTO wallets (user_id, balance, currency, status)
SELECT id, 0.00, 'INR', 'ACTIVE' FROM users WHERE email IN (
    'onkar.demo@payflow.test', 'ananda.demo@payflow.test', 'om.demo@payflow.test',
    'ram.demo@payflow.test', 'amit.demo@payflow.test'
);

INSERT IGNORE INTO spending_limits (user_id, daily_limit, monthly_limit)
SELECT id, 5000.00, 50000.00 FROM users WHERE email IN (
    'onkar.demo@payflow.test', 'ananda.demo@payflow.test', 'om.demo@payflow.test',
    'ram.demo@payflow.test', 'amit.demo@payflow.test'
);
