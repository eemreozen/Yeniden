CREATE SCHEMA IF NOT EXISTS exchange;

CREATE TABLE IF NOT EXISTS exchange.requests (
    id uuid PRIMARY KEY,
    listing_id uuid NOT NULL,
    requester_id uuid NOT NULL,
    owner_id uuid NOT NULL,
    message varchar(500),
    status varchar(255) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp
);

CREATE TABLE IF NOT EXISTS exchange.handovers (
    id uuid PRIMARY KEY,
    request_id uuid NOT NULL,
    listing_id uuid NOT NULL,
    provider_id uuid NOT NULL,
    receiver_id uuid NOT NULL,
    confirmation_code_hash varchar(64) NOT NULL,
    raw_code_for_receiver varchar(6),
    status varchar(255) NOT NULL,
    failed_attempts integer NOT NULL DEFAULT 0,
    expires_at timestamp NOT NULL,
    confirmed_at timestamp,
    created_at timestamp NOT NULL,
    updated_at timestamp
);

ALTER TABLE exchange.handovers ADD COLUMN IF NOT EXISTS category_id uuid;
ALTER TABLE exchange.handovers ADD COLUMN IF NOT EXISTS category_coin_multiplier numeric(4,2);
ALTER TABLE exchange.handovers ADD COLUMN IF NOT EXISTS quantity_band varchar(20);
ALTER TABLE exchange.handovers ADD COLUMN IF NOT EXISTS review_required boolean NOT NULL DEFAULT false;

CREATE TABLE IF NOT EXISTS exchange.handover_outbox_events (
    id uuid PRIMARY KEY,
    event_id uuid NOT NULL UNIQUE,
    event_type varchar(100) NOT NULL,
    aggregate_id uuid NOT NULL,
    payload text NOT NULL,
    created_at timestamp NOT NULL,
    published_at timestamp
);

CREATE INDEX IF NOT EXISTS idx_exchange_outbox_pending
    ON exchange.handover_outbox_events (created_at) WHERE published_at IS NULL;
