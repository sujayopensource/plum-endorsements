-- Phase 3: Error Resolution table
CREATE TABLE error_resolutions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endorsement_id  UUID REFERENCES endorsements(id),
    error_type      VARCHAR(100) NOT NULL,
    original_value  TEXT,
    corrected_value TEXT,
    resolution      TEXT NOT NULL,
    confidence      DECIMAL(5,4) NOT NULL,
    auto_applied    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_error_res_endorsement ON error_resolutions(endorsement_id);
CREATE INDEX idx_error_res_type ON error_resolutions(error_type);
CREATE INDEX idx_error_res_auto_applied ON error_resolutions(auto_applied);
