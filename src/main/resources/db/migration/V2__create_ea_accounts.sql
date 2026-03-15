CREATE TABLE ea_accounts (
    employer_id     UUID NOT NULL,
    insurer_id      UUID NOT NULL,
    balance         DECIMAL(12,2) NOT NULL DEFAULT 0,
    reserved        DECIMAL(12,2) NOT NULL DEFAULT 0,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (employer_id, insurer_id)
);

CREATE TABLE ea_transactions (
    id              BIGSERIAL PRIMARY KEY,
    employer_id     UUID NOT NULL,
    insurer_id      UUID NOT NULL,
    endorsement_id  UUID REFERENCES endorsements(id),
    type            VARCHAR(20) NOT NULL,
    amount          DECIMAL(12,2) NOT NULL,
    balance_after   DECIMAL(12,2) NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_ea_tx_employer ON ea_transactions(employer_id, insurer_id);
CREATE INDEX idx_ea_tx_endorsement ON ea_transactions(endorsement_id);
