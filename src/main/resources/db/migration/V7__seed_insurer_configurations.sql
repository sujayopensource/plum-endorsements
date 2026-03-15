-- V7: Seed additional insurer configurations (ICICI Lombard, Niva Bupa, Bajaj Allianz)

INSERT INTO insurer_configurations (insurer_id, insurer_name, insurer_code, adapter_type,
    supports_real_time, supports_batch, max_batch_size, batch_sla_hours, rate_limit_per_min,
    api_base_url, auth_type, data_format, retry_max_attempts, retry_wait_ms, active)
VALUES
    ('33333333-3333-3333-3333-333333333333', 'ICICI Lombard', 'ICICI_LOMBARD', 'ICICI_LOMBARD',
     true, false, 0, 0, 120,
     'https://api.icicilombard.com/ghi/v1', 'OAUTH2', 'JSON', 3, 1000, true),

    ('44444444-4444-4444-4444-444444444444', 'Niva Bupa', 'NIVA_BUPA', 'NIVA_BUPA',
     false, true, 500, 24, 0,
     'sftp://sftp.nivabupa.com/endorsements', 'SSH_KEY', 'CSV', 3, 2000, true),

    ('55555555-5555-5555-5555-555555555555', 'Bajaj Allianz', 'BAJAJ_ALLIANZ', 'BAJAJ_ALLIANZ',
     true, true, 200, 4, 30,
     'https://api.bajajallianz.com/ws', 'WS_SECURITY', 'XML', 5, 3000, true)
ON CONFLICT (insurer_code) DO NOTHING;
