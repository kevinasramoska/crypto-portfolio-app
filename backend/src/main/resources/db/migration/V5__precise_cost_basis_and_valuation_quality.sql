-- Historical input already rounded to cents cannot be recovered.
ALTER TABLE transactions ALTER COLUMN price_usd TYPE NUMERIC(28,12);
ALTER TABLE transactions ALTER COLUMN realised_profit_usd TYPE NUMERIC(38,20);
ALTER TABLE holdings ALTER COLUMN average_buy_price_usd TYPE NUMERIC(28,12);
ALTER TABLE holdings ADD COLUMN cost_basis_usd NUMERIC(38,20);
UPDATE holdings SET cost_basis_usd = quantity * average_buy_price_usd;
ALTER TABLE transactions ADD COLUMN asset_name VARCHAR(255);
ALTER TABLE holdings ADD COLUMN asset_name VARCHAR(255);
UPDATE transactions SET asset_name = (SELECT name FROM coins WHERE coins.id = transactions.coin_id);
UPDATE holdings SET asset_name = (SELECT name FROM coins WHERE coins.id = holdings.coin_id);
ALTER TABLE portfolio_snapshots ADD COLUMN valuation_complete BOOLEAN NOT NULL DEFAULT FALSE;
-- Existing snapshots have no source freshness evidence and remain uncertified.
