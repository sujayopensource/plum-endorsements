-- Insurer configuration registry for multi-insurer adapter framework
CREATE TABLE insurer_configurations (
    insurer_id          UUID PRIMARY KEY,
    insurer_name        VARCHAR(100) NOT NULL,
    insurer_code        VARCHAR(20) NOT NULL UNIQUE,
    adapter_type        VARCHAR(30) NOT NULL,
    supports_real_time  BOOLEAN NOT NULL DEFAULT false,
    supports_batch      BOOLEAN NOT NULL DEFAULT false,
    max_batch_size      INT NOT NULL DEFAULT 100,
    batch_sla_hours     BIGINT NOT NULL DEFAULT 24,
    rate_limit_per_min  INT NOT NULL DEFAULT 60,
    api_base_url        VARCHAR(500),
    auth_type           VARCHAR(30),
    auth_config         JSONB,
    data_format         VARCHAR(10) NOT NULL DEFAULT 'JSON',
    retry_max_attempts  INT NOT NULL DEFAULT 3,
    retry_wait_ms       BIGINT NOT NULL DEFAULT 2000,
    circuit_breaker_config JSONB,
    active              BOOLEAN NOT NULL DEFAULT true,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_insurer_config_code ON insurer_configurations(insurer_code);
CREATE INDEX idx_insurer_config_active ON insurer_configurations(active);

-- Seed the mock insurer as default (matches existing test data insurer ID)
INSERT INTO insurer_configurations (insurer_id, insurer_name, insurer_code, adapter_type,
    supports_real_time, supports_batch, max_batch_size, batch_sla_hours, rate_limit_per_min,
    data_format, auth_type, active)
VALUES
    ('22222222-2222-2222-2222-222222222222', 'Mock Insurer', 'MOCK', 'MOCK',
     true, true, 100, 24, 60, 'JSON', 'NONE', true);
