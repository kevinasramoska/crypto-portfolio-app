-- Execution time is distinct from the immutable audit-record creation timestamp.
ALTER TABLE transactions ADD COLUMN executed_at TIMESTAMP(6) WITH TIME ZONE;
UPDATE transactions SET executed_at = created_at;
ALTER TABLE transactions ALTER COLUMN executed_at SET NOT NULL;
ALTER TABLE transactions ALTER COLUMN executed_at SET DEFAULT CURRENT_TIMESTAMP;
CREATE INDEX idx_transactions_user_execution_active
    ON transactions (user_id, executed_at, ledger_sequence) WHERE voided_at IS NULL;
