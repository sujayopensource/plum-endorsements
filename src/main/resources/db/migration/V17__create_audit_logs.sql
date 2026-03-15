-- Audit log table for tracking all handler method invocations
CREATE TABLE IF NOT EXISTS audit_logs (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    action       VARCHAR(100)  NOT NULL,
    entity_type  VARCHAR(50)   NOT NULL,
    entity_id    VARCHAR(100),
    actor        VARCHAR(100)  DEFAULT 'SYSTEM',
    details      JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMP     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
