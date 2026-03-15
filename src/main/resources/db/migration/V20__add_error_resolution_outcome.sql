ALTER TABLE error_resolutions ADD COLUMN outcome VARCHAR(20);
ALTER TABLE error_resolutions ADD COLUMN outcome_at TIMESTAMP;
ALTER TABLE error_resolutions ADD COLUMN outcome_endorsement_status VARCHAR(50);
