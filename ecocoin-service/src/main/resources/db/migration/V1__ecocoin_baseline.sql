CREATE SCHEMA IF NOT EXISTS ecocoin;

CREATE TABLE IF NOT EXISTS ecocoin.wallets (
    user_id uuid PRIMARY KEY,
    created_at timestamp NOT NULL,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS ecocoin.transactions (
    id uuid PRIMARY KEY,
    idempotency_key varchar(100) NOT NULL,
    type varchar(255) NOT NULL,
    reason varchar(100),
    source_ref varchar(100),
    counterparty_id uuid,
    beneficiary_id uuid,
    category_id uuid,
    created_by varchar(100),
    created_at timestamp NOT NULL
);

ALTER TABLE ecocoin.transactions ADD COLUMN IF NOT EXISTS reason varchar(100);
ALTER TABLE ecocoin.transactions ADD COLUMN IF NOT EXISTS source_ref varchar(100);
ALTER TABLE ecocoin.transactions ADD COLUMN IF NOT EXISTS counterparty_id uuid;
ALTER TABLE ecocoin.transactions ADD COLUMN IF NOT EXISTS beneficiary_id uuid;
ALTER TABLE ecocoin.transactions ADD COLUMN IF NOT EXISTS category_id uuid;
ALTER TABLE ecocoin.transactions ADD COLUMN IF NOT EXISTS created_by varchar(100);
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'ecocoin' AND table_name = 'transactions' AND column_name = 'description'
    ) THEN
        EXECUTE 'UPDATE ecocoin.transactions SET reason = COALESCE(NULLIF(reason, ''''), NULLIF(description, ''''), ''LEGACY'') WHERE reason IS NULL';
    END IF;
END $$;
UPDATE ecocoin.transactions SET reason = 'LEGACY' WHERE reason IS NULL;
ALTER TABLE ecocoin.transactions ALTER COLUMN reason SET NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_ecocoin_transactions_idempotency
    ON ecocoin.transactions (idempotency_key);

CREATE TABLE IF NOT EXISTS ecocoin.entries (
    id uuid PRIMARY KEY,
    transaction_id uuid NOT NULL,
    account varchar(100) NOT NULL,
    amount bigint NOT NULL,
    created_at timestamp NOT NULL
);
ALTER TABLE ecocoin.entries ALTER COLUMN amount TYPE bigint USING amount::bigint;
CREATE INDEX IF NOT EXISTS idx_ecocoin_entries_account_created
    ON ecocoin.entries (account, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ecocoin_entries_transaction
    ON ecocoin.entries (transaction_id);

CREATE TABLE IF NOT EXISTS ecocoin.outbox_events (
    id uuid PRIMARY KEY,
    event_type varchar(100) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload text NOT NULL,
    created_at timestamp NOT NULL,
    published_at timestamp
);
CREATE INDEX IF NOT EXISTS idx_ecocoin_outbox_pending
    ON ecocoin.outbox_events (created_at) WHERE published_at IS NULL;
