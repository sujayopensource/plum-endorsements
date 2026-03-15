-- Archive tables for data retention policy
CREATE TABLE endorsements_archive (LIKE endorsements INCLUDING ALL);
CREATE TABLE endorsement_events_archive (LIKE endorsement_events INCLUDING ALL);
