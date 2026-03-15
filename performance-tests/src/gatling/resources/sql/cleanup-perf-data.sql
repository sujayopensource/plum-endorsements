-- Cleanup performance test data
-- TRUNCATE in dependency order (child tables first)

TRUNCATE TABLE endorsement_events CASCADE;
TRUNCATE TABLE provisional_coverages CASCADE;
TRUNCATE TABLE endorsement_batches CASCADE;
TRUNCATE TABLE ea_transactions CASCADE;
TRUNCATE TABLE endorsements CASCADE;
TRUNCATE TABLE ea_accounts CASCADE;
