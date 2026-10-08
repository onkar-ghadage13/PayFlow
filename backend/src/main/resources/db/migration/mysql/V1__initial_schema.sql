CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    email VARCHAR(254) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(32) NOT NULL,
    kyc_status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL
);

CREATE TABLE wallets (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    balance DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_wallet_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE payments (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL UNIQUE,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    status VARCHAR(32) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    category VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(100) UNIQUE,
    created_at TIMESTAMP(6) NOT NULL,
    INDEX idx_payments_sender_created (sender_id, created_at),
    INDEX idx_payments_receiver_created (receiver_id, created_at)
);

CREATE TABLE journey_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    transaction_id VARCHAR(36) NOT NULL,
    stage VARCHAR(32) NOT NULL,
    note VARCHAR(500) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    INDEX idx_journey_transaction_time (transaction_id, occurred_at)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    message VARCHAR(300) NOT NULL,
    transaction_id VARCHAR(36) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    INDEX idx_notifications_user_time (user_id, created_at),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users(id)
);
