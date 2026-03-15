-- V8: Create reconciliation tables

CREATE TABLE reconciliation_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    insurer_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING',
    total_checked INT NOT NULL DEFAULT 0,
    matched INT NOT NULL DEFAULT 0,
    partial_matched INT NOT NULL DEFAULT 0,
    rejected INT NOT NULL DEFAULT 0,
    missing INT NOT NULL DEFAULT 0,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_recon_run_insurer FOREIGN KEY (insurer_id) REFERENCES insurer_configurations(insurer_id)
);

CREATE TABLE reconciliation_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    run_id UUID NOT NULL,
    endorsement_id UUID NOT NULL,
    batch_id UUID,
    insurer_id UUID NOT NULL,
    employer_id UUID NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    sent_data JSONB,
    confirmed_data JSONB,
    discrepancy_details JSONB,
    action_taken VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_recon_item_run FOREIGN KEY (run_id) REFERENCES reconciliation_runs(id),
    CONSTRAINT fk_recon_item_endorsement FOREIGN KEY (endorsement_id) REFERENCES endorsements(id)
);

CREATE INDEX idx_recon_runs_insurer ON reconciliation_runs(insurer_id);
CREATE INDEX idx_recon_runs_status ON reconciliation_runs(status);
CREATE INDEX idx_recon_items_run ON reconciliation_items(run_id);
CREATE INDEX idx_recon_items_outcome ON reconciliation_items(outcome);
CREATE INDEX idx_recon_items_endorsement ON reconciliation_items(endorsement_id);
