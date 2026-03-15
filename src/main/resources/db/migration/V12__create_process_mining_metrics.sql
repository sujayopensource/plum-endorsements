-- Phase 3: Process Mining Metrics table
CREATE TABLE process_mining_metrics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurer_id      UUID NOT NULL,
    from_status     VARCHAR(50) NOT NULL,
    to_status       VARCHAR(50) NOT NULL,
    avg_duration_ms BIGINT NOT NULL,
    p95_duration_ms BIGINT NOT NULL,
    p99_duration_ms BIGINT NOT NULL,
    sample_count    INT NOT NULL,
    happy_path_pct  DECIMAL(5,2),
    calculated_at   TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_pm_insurer ON process_mining_metrics(insurer_id);
CREATE INDEX idx_pm_calculated_at ON process_mining_metrics(calculated_at);
CREATE INDEX idx_pm_transition ON process_mining_metrics(from_status, to_status);
