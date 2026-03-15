-- Phase 3: Intelligence Demo Data Seeding

-- Sample anomaly detections (various types and statuses)
INSERT INTO anomaly_detections (id, endorsement_id, employer_id, anomaly_type, score, explanation, flagged_at, reviewed_at, status, reviewer_notes)
VALUES
    ('a1000000-0000-0000-0000-000000000001', NULL, '11111111-1111-1111-1111-111111111111', 'VOLUME_SPIKE', 0.92, 'Volume spike detected: 45 endorsements in 24h vs average of 3/day for employer Acme Corp', NOW() - INTERVAL '2 days', NULL, 'FLAGGED', NULL),
    ('a1000000-0000-0000-0000-000000000002', NULL, '11111111-1111-1111-1111-111111111111', 'ADD_DELETE_CYCLING', 0.85, 'Add/delete cycling detected: employee had both ADD and DELETE endorsements within 30 days', NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', 'UNDER_REVIEW', 'Investigating pattern'),
    ('a1000000-0000-0000-0000-000000000003', NULL, '22222222-2222-2222-2222-222222222222', 'SUSPICIOUS_TIMING', 0.75, 'Suspicious timing: ADD endorsement with coverage starting in 3 days (possible pre-claim addition)', NOW() - INTERVAL '1 day', NULL, 'FLAGGED', NULL),
    ('a1000000-0000-0000-0000-000000000004', NULL, '22222222-2222-2222-2222-222222222222', 'UNUSUAL_PREMIUM', 0.70, 'Unusual premium: Rs 85,000 is 3.2 standard deviations from employer average of Rs 12,500', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days', 'DISMISSED', 'Premium is for family floater plan - expected'),
    ('a1000000-0000-0000-0000-000000000005', NULL, '33333333-3333-3333-3333-333333333333', 'VOLUME_SPIKE', 0.88, 'Volume spike detected: 30 endorsements in 24h vs average of 5/day for employer TechStart Inc', NOW() - INTERVAL '6 hours', NULL, 'FLAGGED', NULL);

-- Sample balance forecasts (one with shortfall)
INSERT INTO balance_forecasts (id, employer_id, insurer_id, forecast_date, forecasted_amount, actual_amount, accuracy, narrative, created_at)
VALUES
    ('b1000000-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', '44444444-4444-4444-4444-444444444444', CURRENT_DATE + INTERVAL '30 days', 280000.00, NULL, NULL, 'Based on 90-day trends (120 ADD endorsements, avg premium Rs 2,333), employer will need approximately Rs 280,000 over the next 30 days. Daily burn rate: Rs 9,333. Confidence: 85%.', NOW() - INTERVAL '1 day'),
    ('b1000000-0000-0000-0000-000000000002', '22222222-2222-2222-2222-222222222222', '44444444-4444-4444-4444-444444444444', CURRENT_DATE + INTERVAL '30 days', 150000.00, 145000.00, 0.9667, 'Based on 90-day trends (65 ADD endorsements, avg premium Rs 2,308), employer will need approximately Rs 150,000 over the next 30 days. Confidence: 82%.', NOW() - INTERVAL '30 days'),
    ('b1000000-0000-0000-0000-000000000003', '33333333-3333-3333-3333-333333333333', '55555555-5555-5555-5555-555555555555', CURRENT_DATE + INTERVAL '30 days', 420000.00, NULL, NULL, 'ALERT: Based on 90-day trends, employer will need Rs 420,000 but current balance is Rs 180,000. Shortfall of Rs 240,000 expected within 12 days. Immediate top-up recommended.', NOW());

-- Sample error resolutions (2 auto-applied, 2 suggested)
INSERT INTO error_resolutions (id, endorsement_id, error_type, original_value, corrected_value, resolution, confidence, auto_applied, created_at)
VALUES
    ('c1000000-0000-0000-0000-000000000001', NULL, 'DATE_FORMAT', '07-03-1990', '1990-03-07', 'Error ''DOB format invalid'' matched pattern ''date_format_mismatch''. ICICI Lombard expects YYYY-MM-DD. Original: ''07-03-1990''. Corrected: ''1990-03-07''. Confidence: 98%.', 0.98, true, NOW() - INTERVAL '2 days'),
    ('c1000000-0000-0000-0000-000000000002', NULL, 'MEMBER_ID_FORMAT', 'abc123', 'PLM-ABC12300', 'Error ''invalid member ID'' matched pattern ''invalid_member_id''. Insurer requires PLM- prefix with 8-char ID. Confidence: 96%.', 0.96, true, NOW() - INTERVAL '1 day'),
    ('c1000000-0000-0000-0000-000000000003', NULL, 'MISSING_FIELD', 'email', 'not-provided@employer.com', 'Error ''required field missing: email'' matched pattern ''required_field_missing''. Suggested default based on employer records. Confidence: 90%.', 0.90, false, NOW() - INTERVAL '3 days'),
    ('c1000000-0000-0000-0000-000000000004', NULL, 'PREMIUM_MISMATCH', '15000', '15750', 'Error ''premium does not match'' matched pattern ''premium_mismatch''. Premium recalculated based on sum-insured table. Confidence: 85%.', 0.85, false, NOW() - INTERVAL '12 hours');

-- Process mining metrics for all 4 insurers
INSERT INTO process_mining_metrics (id, insurer_id, from_status, to_status, avg_duration_ms, p95_duration_ms, p99_duration_ms, sample_count, happy_path_pct, calculated_at)
VALUES
    -- Mock Insurer
    ('d1000000-0000-0000-0000-000000000001', '44444444-4444-4444-4444-444444444444', 'ENDORSEMENT_CREATED', 'ENDORSEMENT_VALIDATED', 250, 500, 800, 150, 92.50, NOW()),
    ('d1000000-0000-0000-0000-000000000002', '44444444-4444-4444-4444-444444444444', 'ENDORSEMENT_VALIDATED', 'ENDORSEMENT_CONFIRMED', 3600000, 7200000, 10800000, 140, 92.50, NOW()),
    -- ICICI Lombard
    ('d1000000-0000-0000-0000-000000000003', '55555555-5555-5555-5555-555555555555', 'ENDORSEMENT_CREATED', 'ENDORSEMENT_VALIDATED', 300, 600, 1000, 80, 88.00, NOW()),
    ('d1000000-0000-0000-0000-000000000004', '55555555-5555-5555-5555-555555555555', 'BATCH_SUBMITTED', 'ENDORSEMENT_CONFIRMED', 65520000, 95040000, 108000000, 75, 88.00, NOW()),
    -- Niva Bupa
    ('d1000000-0000-0000-0000-000000000005', '66666666-6666-6666-6666-666666666666', 'ENDORSEMENT_CREATED', 'ENDORSEMENT_VALIDATED', 280, 550, 900, 60, 85.00, NOW()),
    ('d1000000-0000-0000-0000-000000000006', '66666666-6666-6666-6666-666666666666', 'BATCH_SUBMITTED', 'ENDORSEMENT_CONFIRMED', 72000000, 108000000, 129600000, 55, 85.00, NOW()),
    -- Bajaj Allianz
    ('d1000000-0000-0000-0000-000000000007', '77777777-7777-7777-7777-777777777777', 'ENDORSEMENT_CREATED', 'ENDORSEMENT_VALIDATED', 320, 650, 1100, 45, 90.00, NOW()),
    ('d1000000-0000-0000-0000-000000000008', '77777777-7777-7777-7777-777777777777', 'ENDORSEMENT_SUBMITTED_REALTIME', 'ENDORSEMENT_CONFIRMED', 1800000, 3600000, 5400000, 40, 90.00, NOW());
