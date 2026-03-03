-- ============================================================================
-- V4__add_version_to_accounts.sql
-- Adds optimistic locking version column to accounts table.
-- Prevents concurrent modifications (race conditions) on the same account
-- by detecting conflicting updates at the database level.
-- ============================================================================

ALTER TABLE accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;