CREATE TABLE endorsement_events (
    id              BIGSERIAL PRIMARY KEY,
    endorsement_id  UUID NOT NULL REFERENCES endorsements(id),
    event_type      VARCHAR(50) NOT NULL,
    event_data      JSONB NOT NULL,
    actor           VARCHAR(100),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_endorsement ON endorsement_events(endorsement_id);
CREATE INDEX idx_events_type ON endorsement_events(event_type);
