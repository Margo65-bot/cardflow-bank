CREATE TABLE IF NOT EXISTS transactions (
    id BIGSERIAL PRIMARY KEY,
    from_card_id BIGINT NOT NULL,
    to_card_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transaction_from_card FOREIGN KEY (from_card_id) REFERENCES cards(id),
    CONSTRAINT fk_transaction_to_card FOREIGN KEY (to_card_id) REFERENCES cards(id)
);

CREATE INDEX idx_transactions_from_card ON transactions(from_card_id);