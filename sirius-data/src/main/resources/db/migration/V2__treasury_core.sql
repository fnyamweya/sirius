-- Treasury core schema (market/org isolated)

-- Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Enumerations
DO $$ BEGIN
    CREATE TYPE transfer_status AS ENUM ('PENDING_APPROVAL', 'APPROVED', 'CANCELED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE account_status AS ENUM ('ACTIVE', 'SUSPENDED', 'CLOSED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE ledger_direction AS ENUM ('DEBIT', 'CREDIT');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Accounts
CREATE TABLE IF NOT EXISTS accounts (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    legal_entity_id TEXT NOT NULL,
    currency CHAR(3) NOT NULL,
    status account_status NOT NULL,
    name TEXT NOT NULL,
    external_ref TEXT,
    row_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT accounts_currency_chk CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_accounts_scope ON accounts(market_id, org_id, legal_entity_id);
CREATE INDEX IF NOT EXISTS idx_accounts_external_ref ON accounts(market_id, org_id, external_ref);

-- Account balance read model (fast reads)
CREATE TABLE IF NOT EXISTS account_balance (
    account_id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    legal_entity_id TEXT NOT NULL,
    currency CHAR(3) NOT NULL,
    available_minor BIGINT NOT NULL,
    ledger_minor BIGINT NOT NULL,
    as_of_entry_id UUID,
    row_version BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT account_balance_currency_chk CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_account_balance_scope ON account_balance(market_id, org_id, legal_entity_id);

-- Transfers (aggregate root)
CREATE TABLE IF NOT EXISTS transfers (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    legal_entity_id TEXT NOT NULL,

    source_account_id UUID NOT NULL,
    destination_account_id UUID NOT NULL,

    currency CHAR(3) NOT NULL,
    amount_minor BIGINT NOT NULL,

    status transfer_status NOT NULL,
    reason TEXT,

    created_by_subject TEXT NOT NULL,
    approved_by_subject TEXT,
    canceled_by_subject TEXT,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    row_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT transfers_amount_positive_chk CHECK (amount_minor > 0),
    CONSTRAINT transfers_currency_chk CHECK (char_length(currency) = 3),
    CONSTRAINT transfers_accounts_distinct_chk CHECK (source_account_id <> destination_account_id)
);

CREATE INDEX IF NOT EXISTS idx_transfers_scope ON transfers(market_id, org_id, legal_entity_id);
CREATE INDEX IF NOT EXISTS idx_transfers_status ON transfers(market_id, org_id, status);

-- Idempotency keys for transfer creation
CREATE TABLE IF NOT EXISTS idempotency_keys (
    id BIGSERIAL PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    idempotency_key TEXT NOT NULL,
    request_hash TEXT NOT NULL,
    response_status INT NOT NULL,
    response_body JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_idempotency_scope_key UNIQUE (market_id, org_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_scope ON idempotency_keys(market_id, org_id);

-- Ledger entries (tamper-evident via hash chaining)
CREATE TABLE IF NOT EXISTS ledger_entries (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    legal_entity_id TEXT NOT NULL,

    account_id UUID NOT NULL,
    transfer_id UUID,

    direction ledger_direction NOT NULL,
    currency CHAR(3) NOT NULL,
    amount_minor BIGINT NOT NULL,

    occurred_at TIMESTAMP NOT NULL,

    prev_hash BYTEA,
    entry_hash BYTEA NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ledger_amount_positive_chk CHECK (amount_minor > 0),
    CONSTRAINT ledger_currency_chk CHECK (char_length(currency) = 3)
);

CREATE INDEX IF NOT EXISTS idx_ledger_scope ON ledger_entries(market_id, org_id, legal_entity_id);
CREATE INDEX IF NOT EXISTS idx_ledger_account ON ledger_entries(market_id, org_id, account_id, occurred_at);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ledger_entry_hash ON ledger_entries(entry_hash);

-- Outbox (transactional events)
CREATE TABLE IF NOT EXISTS outbox (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,

    aggregate_type TEXT NOT NULL,
    aggregate_id UUID NOT NULL,
    event_type TEXT NOT NULL,
    payload JSONB NOT NULL,

    dedupe_key TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP,

    CONSTRAINT uq_outbox_dedupe UNIQUE (market_id, org_id, dedupe_key)
);

CREATE INDEX IF NOT EXISTS idx_outbox_unpublished ON outbox(published_at) WHERE published_at IS NULL;

-- Reconciliation runs
DO $$ BEGIN
    CREATE TYPE recon_status AS ENUM ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

CREATE TABLE IF NOT EXISTS reconciliation_runs (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    legal_entity_id TEXT NOT NULL,

    status recon_status NOT NULL,
    started_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP,
    summary JSONB,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_recon_scope ON reconciliation_runs(market_id, org_id, legal_entity_id);

-- Audit events
CREATE TABLE IF NOT EXISTS audit_event (
    id BIGSERIAL PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    legal_entity_id TEXT,

    correlation_id TEXT NOT NULL,
    subject TEXT NOT NULL,
    action TEXT NOT NULL,
    resource_type TEXT NOT NULL,
    resource_id TEXT,
    outcome TEXT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_audit_scope ON audit_event(market_id, org_id, created_at);
