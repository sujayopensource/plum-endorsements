-- Verify data integrity invariants after performance tests

-- 1. EA balance never negative
SELECT employer_id, insurer_id, balance, reserved, (balance - reserved) AS available
FROM ea_accounts
WHERE (balance - reserved) < 0;

-- 2. Endorsement count by status
SELECT status, count(*) AS count
FROM endorsements
GROUP BY status
ORDER BY count DESC;

-- 3. Event count per endorsement (should match expected events for status)
SELECT e.status, avg(event_count) AS avg_events, min(event_count) AS min_events, max(event_count) AS max_events
FROM endorsements e
LEFT JOIN (
    SELECT endorsement_id, count(*) AS event_count
    FROM endorsement_events
    GROUP BY endorsement_id
) ev ON e.id = ev.endorsement_id
GROUP BY e.status
ORDER BY e.status;

-- 4. EA transaction balance consistency
SELECT a.employer_id, a.insurer_id, a.balance, a.reserved,
       COALESCE(t.tx_sum, 0) AS transaction_sum
FROM ea_accounts a
LEFT JOIN (
    SELECT employer_id, insurer_id, sum(amount) AS tx_sum
    FROM ea_transactions
    GROUP BY employer_id, insurer_id
) t ON a.employer_id = t.employer_id AND a.insurer_id = t.insurer_id
WHERE a.reserved != COALESCE(t.tx_sum, 0);

-- 5. Orphaned provisional coverages (endorsement deleted but coverage remains)
SELECT pc.id, pc.endorsement_id
FROM provisional_coverages pc
LEFT JOIN endorsements e ON pc.endorsement_id = e.id
WHERE e.id IS NULL;
