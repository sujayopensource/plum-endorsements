CREATE TABLE endorsement_batches (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurer_id          UUID NOT NULL,
    status              VARCHAR(20) NOT NULL,
    endorsement_count   INT NOT NULL,
    total_premium       DECIMAL(12,2),
    submitted_at        TIMESTAMPTZ,
    sla_deadline        TIMESTAMPTZ,
    insurer_batch_ref   VARCHAR(100),
    response_data       JSONB,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_batches_insurer ON endorsement_batches(insurer_id);
CREATE INDEX idx_batches_status ON endorsement_batches(status);
