-- Seed performance test data
-- 100 EA accounts (10 employers x 10 insurers) with balance=1,000,000

-- Employer UUIDs (must match employers.csv)
-- Insurer UUIDs (must match insurers.csv)

INSERT INTO ea_accounts (employer_id, insurer_id, balance, reserved, updated_at)
SELECT e.eid, i.iid, 1000000.00, 0.00, now()
FROM (VALUES
    ('a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d'::uuid),
    ('b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e'::uuid),
    ('c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f'::uuid),
    ('d4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80'::uuid),
    ('e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091'::uuid),
    ('f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8091a2'::uuid),
    ('07b8c9d0-e1f2-4a3b-4c5d-6e7f8091a2b3'::uuid),
    ('18c9d0e1-f2a3-4b4c-5d6e-7f8091a2b3c4'::uuid),
    ('29d0e1f2-a3b4-4c5d-6e7f-8091a2b3c4d5'::uuid),
    ('3ae1f2a3-b4c5-4d6e-7f80-91a2b3c4d5e6'::uuid)
) AS e(eid)
CROSS JOIN (VALUES
    ('11111111-1111-4111-8111-111111111111'::uuid),
    ('22222222-2222-4222-8222-222222222222'::uuid),
    ('33333333-3333-4333-8333-333333333333'::uuid),
    ('44444444-4444-4444-8444-444444444444'::uuid),
    ('55555555-5555-4555-8555-555555555555'::uuid),
    ('66666666-6666-4666-8666-666666666666'::uuid),
    ('77777777-7777-4777-8777-777777777777'::uuid),
    ('88888888-8888-4888-8888-888888888888'::uuid),
    ('99999999-9999-4999-8999-999999999999'::uuid),
    ('aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid)
) AS i(iid)
ON CONFLICT (employer_id, insurer_id) DO UPDATE SET
    balance = 1000000.00,
    reserved = 0.00,
    updated_at = now();

-- Insert 100 pre-seeded endorsements for read scenarios (various statuses)
DO $$
DECLARE
    emp_ids uuid[] := ARRAY[
        'a1b2c3d4-e5f6-4a7b-8c9d-0e1f2a3b4c5d'::uuid,
        'b2c3d4e5-f6a7-4b8c-9d0e-1f2a3b4c5d6e'::uuid,
        'c3d4e5f6-a7b8-4c9d-0e1f-2a3b4c5d6e7f'::uuid,
        'd4e5f6a7-b8c9-4d0e-1f2a-3b4c5d6e7f80'::uuid,
        'e5f6a7b8-c9d0-4e1f-2a3b-4c5d6e7f8091'::uuid,
        'f6a7b8c9-d0e1-4f2a-3b4c-5d6e7f8091a2'::uuid,
        '07b8c9d0-e1f2-4a3b-4c5d-6e7f8091a2b3'::uuid,
        '18c9d0e1-f2a3-4b4c-5d6e-7f8091a2b3c4'::uuid,
        '29d0e1f2-a3b4-4c5d-6e7f-8091a2b3c4d5'::uuid,
        '3ae1f2a3-b4c5-4d6e-7f80-91a2b3c4d5e6'::uuid
    ];
    ins_ids uuid[] := ARRAY[
        '11111111-1111-4111-8111-111111111111'::uuid,
        '22222222-2222-4222-8222-222222222222'::uuid,
        '33333333-3333-4333-8333-333333333333'::uuid,
        '44444444-4444-4444-8444-444444444444'::uuid,
        '55555555-5555-4555-8555-555555555555'::uuid,
        '66666666-6666-4666-8666-666666666666'::uuid,
        '77777777-7777-4777-8777-777777777777'::uuid,
        '88888888-8888-4888-8888-888888888888'::uuid,
        '99999999-9999-4999-8999-999999999999'::uuid,
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa'::uuid
    ];
    statuses text[] := ARRAY['CREATED', 'VALIDATED', 'PROVISIONALLY_COVERED', 'CONFIRMED', 'REJECTED',
                             'SUBMITTED_REALTIME', 'INSURER_PROCESSING', 'QUEUED_FOR_BATCH', 'BATCH_SUBMITTED', 'FAILED_PERMANENT'];
    types text[] := ARRAY['ADD', 'UPDATE', 'DELETE'];
    i integer;
    emp_idx integer;
    ins_idx integer;
    status_idx integer;
    type_idx integer;
BEGIN
    FOR i IN 1..100 LOOP
        emp_idx := ((i - 1) % 10) + 1;
        ins_idx := (((i - 1) / 10) % 10) + 1;
        status_idx := ((i - 1) % 10) + 1;
        type_idx := ((i - 1) % 3) + 1;

        INSERT INTO endorsements (
            id, employer_id, employee_id, insurer_id, policy_id,
            type, status, coverage_start_date, coverage_end_date,
            employee_data, premium_amount, idempotency_key,
            created_at, updated_at
        ) VALUES (
            gen_random_uuid(),
            emp_ids[emp_idx],
            gen_random_uuid(),
            ins_ids[ins_idx],
            gen_random_uuid(),
            types[type_idx],
            statuses[status_idx],
            CURRENT_DATE + interval '1 day' * (i % 30 + 1),
            CURRENT_DATE + interval '1 day' * (i % 30 + 366),
            jsonb_build_object(
                'name', 'PerfTest Employee ' || i,
                'age', 25 + (i % 40),
                'department', CASE i % 5
                    WHEN 0 THEN 'Engineering'
                    WHEN 1 THEN 'Marketing'
                    WHEN 2 THEN 'Finance'
                    WHEN 3 THEN 'Operations'
                    ELSE 'Human Resources'
                END,
                'email', 'perf.employee.' || i || '@example.com'
            ),
            100.00 + (i * 47.50),
            'perf-seed-' || i,
            now(),
            now()
        ) ON CONFLICT (idempotency_key) DO NOTHING;
    END LOOP;
END $$;
