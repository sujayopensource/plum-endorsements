CREATE TABLE stp_rate_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurer_id UUID NOT NULL,
    snapshot_date DATE NOT NULL,
    total_endorsements INTEGER NOT NULL DEFAULT 0,
    stp_endorsements INTEGER NOT NULL DEFAULT 0,
    stp_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(insurer_id, snapshot_date)
);

CREATE INDEX idx_stp_snapshots_insurer_date ON stp_rate_snapshots(insurer_id, snapshot_date);
