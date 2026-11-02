-- Manual corrections of an authority (US-03, US-07).
--
-- A field report keeps the indicators of the NLP in extracted_indicators and, next to
-- them, the ones an authority corrected, with who, when and why. The corrected ones are
-- used from then on; the original ones stay to tell both apart.
--
-- A zone can be marked as a priority by hand, with who, when and why, for a risk the
-- urgency score does not capture.

ALTER TABLE emergency_management.field_reports
    ADD COLUMN corrected_indicators JSONB,
    ADD COLUMN corrected_missing_variables JSONB,
    ADD COLUMN corrected_by UUID,
    ADD COLUMN corrected_at TIMESTAMP,
    ADD COLUMN correction_reason TEXT,
    ADD CONSTRAINT chk_field_report_correction CHECK (
        (corrected_indicators IS NULL AND corrected_missing_variables IS NULL
            AND corrected_by IS NULL AND corrected_at IS NULL AND correction_reason IS NULL)
        OR (corrected_indicators IS NOT NULL AND extracted_indicators IS NOT NULL
            AND corrected_by IS NOT NULL AND corrected_at IS NOT NULL
            AND correction_reason IS NOT NULL)
    );

ALTER TABLE emergency_management.zones
    ADD COLUMN manual_priority_by UUID,
    ADD COLUMN manual_priority_at TIMESTAMP,
    ADD COLUMN manual_priority_reason TEXT,
    ADD CONSTRAINT chk_zone_manual_priority CHECK (
        (manual_priority_by IS NULL AND manual_priority_at IS NULL
            AND manual_priority_reason IS NULL)
        OR (manual_priority_by IS NOT NULL AND manual_priority_at IS NOT NULL
            AND manual_priority_reason IS NOT NULL)
    );

COMMENT ON COLUMN emergency_management.field_reports.corrected_indicators IS 'Indicators corrected by an authority (US-03); NULL while nobody corrected them. extracted_indicators keeps those of the NLP.';
COMMENT ON COLUMN emergency_management.field_reports.corrected_by IS 'AuthorityId from Identity Access who last corrected the indicators. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN emergency_management.zones.manual_priority_by IS 'AuthorityId from Identity Access who marked the zone as a priority by hand (US-07). Logical reference: not a foreign key because the data lives in another schema.';
