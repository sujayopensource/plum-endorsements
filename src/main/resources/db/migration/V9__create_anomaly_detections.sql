-- Phase 3: Anomaly Detection table
CREATE TABLE anomaly_detections (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endorsement_id  UUID REFERENCES endorsements(id),
    employer_id     UUID NOT NULL,
    anomaly_type    VARCHAR(50) NOT NULL,
    score           DECIMAL(5,4) NOT NULL,
    explanation     TEXT NOT NULL,
    flagged_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reviewed_at     TIMESTAMPTZ,
    status          VARCHAR(30) NOT NULL DEFAULT 'FLAGGED',
    reviewer_notes  TEXT
);

CREATE INDEX idx_anomaly_employer ON anomaly_detections(employer_id);
CREATE INDEX idx_anomaly_status ON anomaly_detections(status);
CREATE INDEX idx_anomaly_flagged_at ON anomaly_detections(flagged_at);
CREATE INDEX idx_anomaly_type ON anomaly_detections(anomaly_type);
