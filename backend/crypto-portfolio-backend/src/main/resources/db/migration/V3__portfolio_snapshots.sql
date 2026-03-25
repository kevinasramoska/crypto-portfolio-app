-- V3__portfolio_snapshots.sql
-- Create table for storing daily portfolio snapshots

CREATE TABLE portfolio_snapshots (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    snapshot_date DATE NOT NULL,
    total_value_usd NUMERIC(19,2) NOT NULL,
    total_invested_usd NUMERIC(19,2) NOT NULL,
    unrealized_pnl_usd NUMERIC(19,2) NOT NULL,
    UNIQUE (user_id, snapshot_date)
);
