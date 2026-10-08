CREATE TABLE spending_limits (
    user_id BIGINT NOT NULL PRIMARY KEY,
    daily_limit DECIMAL(19,2) NOT NULL DEFAULT 5000.00,
    monthly_limit DECIMAL(19,2) NOT NULL DEFAULT 50000.00,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_spending_limits_user FOREIGN KEY (user_id) REFERENCES users(id)
);

INSERT INTO spending_limits (user_id)
SELECT id FROM users;
