-- Indexes + partial indexes for rich treasury schema

-- Accounts
CREATE INDEX IF NOT EXISTS idx_accounts_market_org_created_at ON accounts(market_id, org_id, created_at);
CREATE INDEX IF NOT EXISTS idx_accounts_market_org_type ON accounts(market_id, org_id, account_type);

-- Account balance
CREATE INDEX IF NOT EXISTS idx_account_balance_market_org_updated ON account_balance(market_id, org_id, updated_at);

-- Transfers
CREATE INDEX IF NOT EXISTS idx_transfers_market_org_created_at ON transfers(market_id, org_id, created_at);
CREATE INDEX IF NOT EXISTS idx_transfers_market_org_status ON transfers(market_id, org_id, status);
CREATE INDEX IF NOT EXISTS idx_transfers_market_org_type ON transfers(market_id, org_id, transfer_type);

-- Outbox
CREATE INDEX IF NOT EXISTS idx_outbox_market_org_unpublished ON outbox(market_id, org_id, created_at) WHERE published_at IS NULL;

-- Journal
CREATE INDEX IF NOT EXISTS idx_journal_entries_scope_posted ON journal_entries(market_id, org_id, posted_at);
CREATE INDEX IF NOT EXISTS idx_journal_entries_reference ON journal_entries(reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_journal_lines_entry_id ON journal_lines(entry_id);
CREATE INDEX IF NOT EXISTS idx_journal_lines_account_posted ON journal_lines(account_id, created_at);

-- Reconciliation
CREATE INDEX IF NOT EXISTS idx_recon_runs_scope_started ON reconciliation_runs(market_id, org_id, started_at);
CREATE INDEX IF NOT EXISTS idx_ext_stmt_scope_ref ON external_statement_line(market_id, org_id, source_ref);

-- Org config
CREATE INDEX IF NOT EXISTS idx_org_config_scope ON org_config(market_id, org_id);
