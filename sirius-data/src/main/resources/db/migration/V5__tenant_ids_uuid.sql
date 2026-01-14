-- Convert tenancy identifiers (org_id, legal_entity_id) from TEXT to UUID.
--
-- IMPORTANT: this migration expects existing values to already be UUID strings.
-- If any rows contain non-UUID values (e.g. 'ORG1'), the cast will fail and the migration will stop.

-- Core tables
ALTER TABLE accounts
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid,
    ALTER COLUMN legal_entity_id TYPE UUID USING legal_entity_id::uuid;

ALTER TABLE account_balance
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid,
    ALTER COLUMN legal_entity_id TYPE UUID USING legal_entity_id::uuid;

ALTER TABLE transfers
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid,
    ALTER COLUMN legal_entity_id TYPE UUID USING legal_entity_id::uuid;

ALTER TABLE ledger_entries
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid,
    ALTER COLUMN legal_entity_id TYPE UUID USING legal_entity_id::uuid;

ALTER TABLE outbox
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid;

ALTER TABLE idempotency_keys
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid;

ALTER TABLE reconciliation_runs
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid,
    ALTER COLUMN legal_entity_id TYPE UUID USING legal_entity_id::uuid;

ALTER TABLE audit_event
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid,
    ALTER COLUMN legal_entity_id TYPE UUID USING legal_entity_id::uuid;

-- Journal (rich ledger)
ALTER TABLE journal_entries
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid;

-- Reconciliation supporting tables
ALTER TABLE external_statement_line
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid;

-- Org configuration
ALTER TABLE org_config
    ALTER COLUMN org_id TYPE UUID USING org_id::uuid;
