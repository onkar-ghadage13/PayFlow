-- Run this in MySQL Workbench after connecting to the PayFlow database.
USE payflow;

-- Full transaction history, newest first, with sender and receiver details.
SELECT
    p.transaction_id,
    p.created_at,
    sender.name AS sender_name,
    sender.email AS sender_email,
    receiver.name AS receiver_name,
    receiver.email AS receiver_email,
    p.amount,
    COALESCE(sender_wallet.currency, receiver_wallet.currency, 'INR') AS currency,
    p.status,
    p.description,
    p.category
FROM payments p
LEFT JOIN users sender ON sender.id = p.sender_id
LEFT JOIN users receiver ON receiver.id = p.receiver_id
LEFT JOIN wallets sender_wallet ON sender_wallet.user_id = p.sender_id
LEFT JOIN wallets receiver_wallet ON receiver_wallet.user_id = p.receiver_id
ORDER BY p.created_at DESC, p.id DESC;

-- Lifecycle timeline for every transaction.
SELECT
    p.transaction_id,
    p.created_at AS transaction_created_at,
    p.status AS current_status,
    e.stage AS journey_stage,
    e.note,
    e.occurred_at AS event_time
FROM payments p
LEFT JOIN journey_events e ON e.transaction_id = p.transaction_id
ORDER BY p.created_at DESC, p.id DESC, e.occurred_at ASC, e.id ASC;
