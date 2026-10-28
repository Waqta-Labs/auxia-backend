-- Distribution plans (US-11 to US-14). A plan belongs to the organization whose
-- inventory it uses, and records how much of the need of its zone it covers.
-- It keeps the original recommendation next to the version the authority
-- approves (US-13, scenario 2), and who changed its quantities, when and why
-- (US-14). No plan was stored before this version.

ALTER TABLE emergency_management.distribution_plans
    ADD COLUMN organization_id UUID NOT NULL,
    ADD COLUMN coverage VARCHAR(7) NOT NULL,
    ADD COLUMN modified_by UUID,
    ADD COLUMN modified_at TIMESTAMP,
    ADD COLUMN modification_reason TEXT,
    -- A zone that would receive nothing (NONE) gets no plan.
    ADD CONSTRAINT chk_distribution_plan_coverage CHECK (coverage IN ('FULL', 'PARTIAL')),
    ADD CONSTRAINT chk_distribution_plan_modification CHECK (
        (modified_by IS NULL AND modified_at IS NULL AND modification_reason IS NULL)
        OR (modified_by IS NOT NULL AND modified_at IS NOT NULL
            AND modification_reason IS NOT NULL)
    );

CREATE INDEX idx_distribution_plans_emergency_organization
    ON emergency_management.distribution_plans (emergency_id, organization_id, status);

-- A quantity of 0 takes a recommended resource out of the plan without losing
-- what was recommended; a resource the authority adds has no recommended quantity.
ALTER TABLE emergency_management.distribution_items
    ADD COLUMN recommended_quantity INTEGER,
    DROP CONSTRAINT distribution_items_quantity_check,
    ADD CONSTRAINT chk_distribution_item_quantity CHECK (
        quantity >= 0
        AND (recommended_quantity IS NULL OR recommended_quantity > 0)
        AND (quantity > 0 OR recommended_quantity IS NOT NULL)
    );

COMMENT ON COLUMN emergency_management.distribution_plans.organization_id IS 'OrganizationId from Identity Access: the organization whose inventory the plan reserves. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN emergency_management.distribution_plans.coverage IS 'FULL or PARTIAL: how much of the need of the zone the recommendation covers (US-11, scenario 2).';
COMMENT ON COLUMN emergency_management.distribution_plans.modified_by IS 'AuthorityId from Identity Access who last changed the quantities, with modified_at and modification_reason (US-14).';
COMMENT ON COLUMN emergency_management.distribution_items.recommended_quantity IS 'Quantity of the original recommendation; NULL for a resource the authority added (US-13, scenario 2).';
