-- Treasury bounded contexts v3 (rich domain foundations)

-- Expand enums (PostgreSQL enums are append-only; keep backward compatibility)
DO $$ BEGIN
    ALTER TYPE transfer_status ADD VALUE IF NOT EXISTS 'DRAFT';
    ALTER TYPE transfer_status ADD VALUE IF NOT EXISTS 'QUEUED';
    ALTER TYPE transfer_status ADD VALUE IF NOT EXISTS 'PROCESSING';
    ALTER TYPE transfer_status ADD VALUE IF NOT EXISTS 'COMPLETED';
    ALTER TYPE transfer_status ADD VALUE IF NOT EXISTS 'FAILED';
EXCEPTION
    WHEN undefined_object THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TYPE account_status ADD VALUE IF NOT EXISTS 'FROZEN';
EXCEPTION
    WHEN undefined_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE account_type AS ENUM ('OPERATING','SETTLEMENT','SAFEGUARDING','RESERVE','FEE','SUSPENSE');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE transfer_type AS ENUM ('INTERNAL','EXTERNAL','SWEEP','REVERSAL');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE journal_status AS ENUM ('POSTED','REVERSED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE journal_line_type AS ENUM ('PRINCIPAL','FEE','FX','REVERSAL');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE recon_state AS ENUM ('STARTED','RUNNING','COMPLETED','FAILED');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- Accounts: extend for type/state + overdraft policy
ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS account_type account_type NOT NULL DEFAULT 'OPERATING',
    ADD COLUMN IF NOT EXISTS allow_overdraft BOOLEAN NOT NULL DEFAULT false;

-- Account balance read model: reserved + pending
ALTER TABLE account_balance
    ADD COLUMN IF NOT EXISTS reserved_minor BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS pending_minor BIGINT NOT NULL DEFAULT 0;

-- Transfers: add type + lifecycle timestamps + failure/cancel details
ALTER TABLE transfers
    ADD COLUMN IF NOT EXISTS transfer_type transfer_type NOT NULL DEFAULT 'INTERNAL',
    ADD COLUMN IF NOT EXISTS queued_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processing_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS failed_reason TEXT;

-- Immutable double-entry ledger
CREATE TABLE IF NOT EXISTS journal_entries (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    correlation_id TEXT NOT NULL,
    reference_type TEXT NOT NULL,
    reference_id UUID NOT NULL,
    status journal_status NOT NULL,
    posted_at TIMESTAMP NOT NULL,
    prev_hash BYTEA,
    entry_hash BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_journal_entry_hash UNIQUE (entry_hash)
);

CREATE TABLE IF NOT EXISTS journal_lines (
    id UUID PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES journal_entries(id),
    account_id UUID NOT NULL REFERENCES accounts(id),
    currency CHAR(3) NOT NULL,
    direction ledger_direction NOT NULL,
    line_type journal_line_type NOT NULL,
    amount NUMERIC(20,6) NOT NULL,
    memo TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT journal_lines_amount_positive_chk CHECK (amount > 0),
    CONSTRAINT journal_lines_currency_chk CHECK (char_length(currency) = 3)
);

CREATE TABLE IF NOT EXISTS journal_entry_reversals (
    id UUID PRIMARY KEY,
    original_entry_id UUID NOT NULL REFERENCES journal_entries(id),
    reversal_entry_id UUID NOT NULL REFERENCES journal_entries(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_reversal_original UNIQUE (original_entry_id),
    CONSTRAINT uq_reversal_reversal UNIQUE (reversal_entry_id)
);

-- Reconciliation
ALTER TABLE reconciliation_runs
    ADD COLUMN IF NOT EXISTS source_ref TEXT,
    ADD COLUMN IF NOT EXISTS state recon_state,
    ADD COLUMN IF NOT EXISTS metrics JSONB,
    ADD COLUMN IF NOT EXISTS ended_at TIMESTAMP;

UPDATE reconciliation_runs
SET state = 'COMPLETED'
WHERE state IS NULL AND status = 'COMPLETED';

UPDATE reconciliation_runs
SET state = 'FAILED'
WHERE state IS NULL AND status = 'FAILED';

UPDATE reconciliation_runs
SET state = 'RUNNING'
WHERE state IS NULL AND status = 'RUNNING';

UPDATE reconciliation_runs
SET state = 'STARTED'
WHERE state IS NULL AND status = 'PENDING';

ALTER TABLE reconciliation_runs
    ALTER COLUMN state SET NOT NULL;

CREATE TABLE IF NOT EXISTS external_statement_line (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    source_ref TEXT NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    currency CHAR(3) NOT NULL,
    amount NUMERIC(20,6) NOT NULL,
    beneficiary TEXT,
    reference TEXT,
    raw JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT ext_stmt_amount_positive_chk CHECK (amount > 0),
    CONSTRAINT ext_stmt_currency_chk CHECK (char_length(currency) = 3)
);

-- Org config / products (policy bundles)
CREATE TABLE IF NOT EXISTS products (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL,
    description TEXT,
    policy_bundle JSONB NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_products_name UNIQUE (name)
);

CREATE TABLE IF NOT EXISTS org_config (
    id UUID PRIMARY KEY,
    market_id TEXT NOT NULL,
    org_id TEXT NOT NULL,
    product_id UUID REFERENCES products(id),
    version BIGINT NOT NULL DEFAULT 0,
    feature_flags JSONB NOT NULL DEFAULT '{}'::jsonb,
    limits JSONB NOT NULL DEFAULT '{}'::jsonb,
    approval_thresholds JSONB NOT NULL DEFAULT '{}'::jsonb,
    fee_schedule JSONB NOT NULL DEFAULT '{}'::jsonb,
    routing_policy JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_org_config_scope UNIQUE (market_id, org_id)
);
