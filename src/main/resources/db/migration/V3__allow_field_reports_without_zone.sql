-- A field report always belongs to an emergency and its zone is optional:
-- a report whose zone cannot be identified stays pending zone assignment
-- (US-01, scenario 2). FieldReport becomes its own aggregate and references
-- its emergency and its zone by identifier.

ALTER TABLE emergency_management.field_reports
    ADD COLUMN emergency_id UUID;

UPDATE emergency_management.field_reports AS report
SET emergency_id = zone.emergency_id
FROM emergency_management.zones AS zone
WHERE zone.id = report.zone_id;

ALTER TABLE emergency_management.field_reports
    ALTER COLUMN emergency_id SET NOT NULL,
    ALTER COLUMN zone_id DROP NOT NULL,
    DROP CONSTRAINT field_reports_zone_id_fkey,
    ADD CONSTRAINT fk_field_report_emergency FOREIGN KEY (emergency_id)
        REFERENCES emergency_management.emergencies (id) ON DELETE RESTRICT,
    -- Like distribution_plans: a report cannot point to a zone of another emergency.
    ADD CONSTRAINT fk_field_report_zone FOREIGN KEY (zone_id, emergency_id)
        REFERENCES emergency_management.zones (id, emergency_id) ON DELETE RESTRICT;

CREATE INDEX idx_field_reports_emergency_id ON emergency_management.field_reports (emergency_id);
CREATE INDEX idx_field_reports_pending_zone ON emergency_management.field_reports (emergency_id)
    WHERE zone_id IS NULL;

COMMENT ON TABLE emergency_management.field_reports IS 'Aggregate Root FieldReport. It always belongs to an emergency; zone_id is NULL while the report is pending zone assignment (US-01). A report is processed when extracted_indicators is not NULL (StructuredFieldData).';
