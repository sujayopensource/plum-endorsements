CREATE TABLE endorsements (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employer_id         UUID NOT NULL,
    employee_id         UUID NOT NULL,
    insurer_id          UUID NOT NULL,
    policy_id           UUID NOT NULL,
    type                VARCHAR(20) NOT NULL,
    status              VARCHAR(30) NOT NULL,
    coverage_start_date DATE NOT NULL,
    coverage_end_date   DATE,
    employee_data       JSONB NOT NULL,
    premium_amount      DECIMAL(12,2),
    batch_id            UUID,
    insurer_reference   VARCHAR(100),
    retry_count         INT DEFAULT 0,
    failure_reason      TEXT,
    idempotency_key     VARCHAR(100) UNIQUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    version             INT DEFAULT 1
);

CREATE INDEX idx_endorsements_employer ON endorsements(employer_id);
CREATE INDEX idx_endorsements_employee ON endorsements(employee_id);
CREATE INDEX idx_endorsements_status ON endorsements(status);
CREATE INDEX idx_endorsements_batch ON endorsements(batch_id);
CREATE INDEX idx_endorsements_insurer ON endorsements(insurer_id);
CREATE INDEX idx_endorsements_created ON endorsements(created_at);
