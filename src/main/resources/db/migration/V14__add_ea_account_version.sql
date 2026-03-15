-- Optimistic locking for concurrent EA Account updates
ALTER TABLE ea_accounts ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
