CREATE TABLE provisional_coverages (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    endorsement_id  UUID NOT NULL REFERENCES endorsements(id),
    employee_id     UUID NOT NULL,
    employer_id     UUID NOT NULL,
    coverage_start  DATE NOT NULL,
    coverage_type   VARCHAR(20) DEFAULT 'PROVISIONAL',
    confirmed_at    TIMESTAMPTZ,
    expired_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_prov_cov_endorsement ON provisional_coverages(endorsement_id);
CREATE INDEX idx_prov_cov_employee ON provisional_coverages(employee_id);
CREATE INDEX idx_prov_cov_employer ON provisional_coverages(employer_id);
