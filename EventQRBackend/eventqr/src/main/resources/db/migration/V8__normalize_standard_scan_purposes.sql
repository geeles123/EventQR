UPDATE scan_purposes
SET code = 'REGISTRATION_LOOKUP',
    tracking_only = true,
    description = 'Verify attendee registration.'
WHERE lower(name) = 'verification'
  AND code = 'ID_PRINT';

INSERT INTO scan_purposes (event_id, name, code, active, tracking_only, description, created_at, updated_at)
SELECT e.id, 'ID Print', 'ID_PRINT', true, true, 'Verify attendee for ID printing.', now(), now()
FROM events e
WHERE NOT EXISTS (
    SELECT 1
    FROM scan_purposes sp
    WHERE sp.event_id = e.id
      AND sp.code = 'ID_PRINT'
);

INSERT INTO transaction_rules (event_id, scan_purpose_id, active, allow_duplicate, requires_staff_assignment, points_awarded, duplicate_window_minutes, max_uses_per_registration, rule_config, created_at, updated_at)
SELECT sp.event_id, sp.id, true, false, true, 0, 0, 1, '{}'::jsonb, now(), now()
FROM scan_purposes sp
WHERE NOT EXISTS (
    SELECT 1
    FROM transaction_rules tr
    WHERE tr.event_id = sp.event_id
      AND tr.scan_purpose_id = sp.id
);
