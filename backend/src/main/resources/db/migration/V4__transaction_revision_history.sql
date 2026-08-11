ALTER TABLE transactions
    ADD COLUMN ledger_sequence BIGINT,
    ADD COLUMN voided_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN replacement_transaction_id BIGINT;

WITH sequenced_transactions AS (
    SELECT id,
           ROW_NUMBER() OVER (PARTITION BY user_id ORDER BY created_at, id) AS ledger_sequence
    FROM transactions
)
UPDATE transactions
SET ledger_sequence = sequenced_transactions.ledger_sequence
FROM sequenced_transactions
WHERE transactions.id = sequenced_transactions.id;

ALTER TABLE transactions
    ALTER COLUMN ledger_sequence SET NOT NULL,
    ADD CONSTRAINT chk_transactions_ledger_sequence_positive CHECK (ledger_sequence > 0),
    ADD CONSTRAINT fk_transactions_replacement
        FOREIGN KEY (replacement_transaction_id) REFERENCES transactions(id);

CREATE INDEX idx_transactions_user_active_ledger_sequence
    ON transactions (user_id, ledger_sequence DESC)
    WHERE voided_at IS NULL;

CREATE UNIQUE INDEX uq_transactions_user_active_ledger_sequence
    ON transactions (user_id, ledger_sequence)
    WHERE voided_at IS NULL;

CREATE UNIQUE INDEX uq_transactions_replacement
    ON transactions (replacement_transaction_id)
    WHERE replacement_transaction_id IS NOT NULL;

ALTER TABLE holdings
    ADD CONSTRAINT uq_holdings_user_coin UNIQUE (user_id, coin_id);
