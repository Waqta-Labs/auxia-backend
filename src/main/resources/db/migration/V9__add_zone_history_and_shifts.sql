-- History of decisions of each zone and shift changes between authorities (US-20, US-23).
-- Each zone has a responsible authority: the one who registered it, until a shift change
-- passes it to the next one.

ALTER TABLE emergency_management.zones
    ADD COLUMN responsible_authority_id UUID;

CREATE INDEX idx_zones_responsible_authority
    ON emergency_management.zones (responsible_authority_id)
    WHERE responsible_authority_id IS NOT NULL;

COMMENT ON COLUMN emergency_management.zones.responsible_authority_id IS 'AuthorityId from Identity Access who follows the zone (US-23). NULL for the zones registered before this migration. Logical reference: not a foreign key because the data lives in another schema.';

CREATE TABLE emergency_management.shift_handovers (
    id                     UUID PRIMARY KEY,
    organization_id        UUID NOT NULL,
    outgoing_authority_id  UUID NOT NULL,
    incoming_authority_id  UUID NOT NULL,
    handed_over_at         TIMESTAMP NOT NULL,
    notes                  TEXT,
    CONSTRAINT chk_shift_handover_authorities
        CHECK (outgoing_authority_id <> incoming_authority_id)
);

CREATE TABLE emergency_management.shift_handover_zones (
    shift_handover_id  UUID NOT NULL
        REFERENCES emergency_management.shift_handovers (id) ON DELETE CASCADE,
    zone_id            UUID NOT NULL
        REFERENCES emergency_management.zones (id) ON DELETE RESTRICT,
    PRIMARY KEY (shift_handover_id, zone_id)
);

COMMENT ON TABLE emergency_management.shift_handovers IS 'Aggregate ShiftHandover. A shift change between two authorities of the same organization (US-23).';
COMMENT ON TABLE emergency_management.shift_handover_zones IS 'Zones of active emergencies that passed to the incoming authority.';

CREATE TABLE emergency_management.zone_decisions (
    id              UUID PRIMARY KEY,
    zone_id         UUID NOT NULL
        REFERENCES emergency_management.zones (id) ON DELETE CASCADE,
    type            VARCHAR(30) NOT NULL CHECK (type IN (
        'ZONE_REGISTERED', 'PLAN_MODIFIED', 'PLAN_APPROVED', 'PLAN_REJECTED',
        'MANUAL_PRIORITY_SET', 'MANUAL_PRIORITY_REMOVED', 'REPORT_CORRECTED',
        'SHIFT_HANDED_OVER')),
    decided_by      UUID NOT NULL,
    decided_at      TIMESTAMP NOT NULL,
    detail          TEXT,
    subject_id      UUID,
    handed_over_to  UUID,
    CONSTRAINT chk_zone_decision_handover
        CHECK ((type = 'SHIFT_HANDED_OVER') = (handed_over_to IS NOT NULL))
);

CREATE INDEX idx_zone_decisions_zone
    ON emergency_management.zone_decisions (zone_id, decided_at);

COMMENT ON TABLE emergency_management.zone_decisions IS 'Aggregate ZoneDecision. History of decisions of a zone (US-20, TS-C01); rows are only inserted.';
COMMENT ON COLUMN emergency_management.zone_decisions.subject_id IS 'Plan, field report or shift handover the decision is about, depending on the type.';

-- The decisions taken before this migration that the data still keeps: the last
-- modification and the decision of each plan, the correction of each report and the
-- current manual priority. Who registered a zone, and what was replaced, is not known.
INSERT INTO emergency_management.zone_decisions
    (id, zone_id, type, decided_by, decided_at, detail, subject_id)
SELECT gen_random_uuid(), zone_id, 'PLAN_MODIFIED', modified_by, modified_at,
       modification_reason, id
FROM emergency_management.distribution_plans
WHERE modified_by IS NOT NULL
UNION ALL
SELECT gen_random_uuid(), zone_id,
       CASE WHEN status = 'REJECTED' THEN 'PLAN_REJECTED' ELSE 'PLAN_APPROVED' END,
       approved_by, approved_at, rejected_reason, id
FROM emergency_management.distribution_plans
WHERE status IN ('APPROVED', 'EXECUTED', 'REJECTED')
UNION ALL
SELECT gen_random_uuid(), zone_id, 'REPORT_CORRECTED', corrected_by, corrected_at,
       correction_reason, id
FROM emergency_management.field_reports
WHERE corrected_by IS NOT NULL AND zone_id IS NOT NULL
UNION ALL
SELECT gen_random_uuid(), id, 'MANUAL_PRIORITY_SET', manual_priority_by, manual_priority_at,
       manual_priority_reason, NULL
FROM emergency_management.zones
WHERE manual_priority_by IS NOT NULL;
