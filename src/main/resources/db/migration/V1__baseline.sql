-- =========================================================
-- AuxIA - Baseline: the schema of the five bounded contexts.
-- It is the concatenation of the scripts of the project report
-- (assets/plant/SQL), already applied on Neon before Flyway.
-- On Neon, Flyway records this version as the baseline and does not run it.
-- =========================================================

-- =========================================================
-- AuxIA - Bounded Context: Emergency Management
-- Engine: PostgreSQL 18 (Neon)
-- One schema per bounded context inside the same database.
-- =========================================================

CREATE SCHEMA IF NOT EXISTS emergency_management;

-- ---------------------------------------------------------
-- emergencies (Aggregate Root: Emergency)
-- ---------------------------------------------------------
CREATE TABLE emergency_management.emergencies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    type            VARCHAR(20)  NOT NULL
        CHECK (type IN ('EARTHQUAKE', 'FLOOD', 'LANDSLIDE', 'FIRE', 'OTHER')),
    description     TEXT,
    department      VARCHAR(100) NOT NULL,
    province        VARCHAR(100) NOT NULL,
    district        VARCHAR(100) NOT NULL,
    status          VARCHAR(10)  NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'CLOSED')),
    start_date      DATE         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

COMMENT ON TABLE emergency_management.emergencies IS 'Aggregate Root Emergency. An emergency is closed (status = CLOSED), never deleted.';

-- ---------------------------------------------------------
-- zones (Aggregate Root: Zone)
-- ---------------------------------------------------------
CREATE TABLE emergency_management.zones (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emergency_id            UUID NOT NULL
        REFERENCES emergency_management.emergencies (id) ON DELETE RESTRICT,
    name                    VARCHAR(150) NOT NULL,
    latitude                DOUBLE PRECISION,
    longitude               DOUBLE PRECISION,
    families_affected       INTEGER NOT NULL DEFAULT 0 CHECK (families_affected >= 0),
    people_affected         INTEGER NOT NULL DEFAULT 0 CHECK (people_affected >= 0),
    children                INTEGER NOT NULL DEFAULT 0 CHECK (children >= 0),
    elderly_people          INTEGER NOT NULL DEFAULT 0 CHECK (elderly_people >= 0),
    disabled_people         INTEGER NOT NULL DEFAULT 0 CHECK (disabled_people >= 0),
    damage_level            VARCHAR(10) NOT NULL
        CHECK (damage_level IN ('LOW', 'MODERATE', 'SEVERE', 'CRITICAL')),
    urgency_score           NUMERIC(5,2) CHECK (urgency_score BETWEEN 0 AND 100),
    urgency_level           VARCHAR(10)
        CHECK (urgency_level IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    urgency_top_features    JSONB,
    urgency_model_version   VARCHAR(30),
    urgency_calculated_at   TIMESTAMP,
    status                  VARCHAR(15) NOT NULL DEFAULT 'REGISTERED'
        CHECK (status IN ('REGISTERED', 'PRIORITIZED', 'SERVED')),
    last_aid_received_at    TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_zone_emergency UNIQUE (id, emergency_id),
    CONSTRAINT chk_zone_urgency_score CHECK (
        (urgency_score IS NULL AND urgency_level IS NULL AND urgency_model_version IS NULL AND urgency_calculated_at IS NULL)
        OR (urgency_score IS NOT NULL AND urgency_level IS NOT NULL AND urgency_model_version IS NOT NULL AND urgency_calculated_at IS NOT NULL)
    )
);

CREATE INDEX idx_zones_emergency_id ON emergency_management.zones (emergency_id);
CREATE INDEX idx_zones_status ON emergency_management.zones (status);

COMMENT ON TABLE emergency_management.zones IS 'Aggregate Root Zone. The urgency_* columns store the UrgencyScore value object: either all empty or all filled.';
COMMENT ON COLUMN emergency_management.zones.urgency_top_features IS 'Variables with the greatest influence on the score (UrgencyScore.topFeatures).';

-- ---------------------------------------------------------
-- field_reports (Entity inside the Zone aggregate)
-- ---------------------------------------------------------
CREATE TABLE emergency_management.field_reports (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    zone_id                 UUID NOT NULL
        REFERENCES emergency_management.zones (id) ON DELETE CASCADE,
    raw_text                TEXT NOT NULL,
    source                  VARCHAR(15) NOT NULL
        CHECK (source IN ('AUTHORITY', 'VOLUNTEER', 'CITIZEN')),
    extracted_indicators    JSONB,
    missing_variables       JSONB,
    reported_at             TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_field_report_structured_data CHECK (
        missing_variables IS NULL OR extracted_indicators IS NOT NULL
    )
);

CREATE INDEX idx_field_reports_zone_id ON emergency_management.field_reports (zone_id);

COMMENT ON TABLE emergency_management.field_reports IS 'Entity FieldReport. A report is processed when extracted_indicators is not NULL (StructuredFieldData).';

-- ---------------------------------------------------------
-- distribution_plans (Aggregate Root: DistributionPlan)
-- ---------------------------------------------------------
CREATE TABLE emergency_management.distribution_plans (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    emergency_id        UUID NOT NULL,
    zone_id             UUID NOT NULL,
    status              VARCHAR(10) NOT NULL DEFAULT 'PROPOSED'
        CHECK (status IN ('PROPOSED', 'APPROVED', 'REJECTED', 'EXECUTED')),
    justification       TEXT,
    model_version       VARCHAR(30),
    approved_by         UUID,
    approved_at         TIMESTAMP,
    rejected_reason     TEXT,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT fk_distribution_plan_zone FOREIGN KEY (zone_id, emergency_id)
        REFERENCES emergency_management.zones (id, emergency_id) ON DELETE RESTRICT,
    CONSTRAINT chk_distribution_plan_decision CHECK (
        (status = 'PROPOSED'
            AND approved_by IS NULL AND approved_at IS NULL AND rejected_reason IS NULL)
        OR (status IN ('APPROVED', 'EXECUTED')
            AND approved_by IS NOT NULL AND approved_at IS NOT NULL AND rejected_reason IS NULL)
        OR (status = 'REJECTED'
            AND approved_by IS NOT NULL AND approved_at IS NOT NULL AND rejected_reason IS NOT NULL)
    )
);

CREATE INDEX idx_distribution_plans_zone_id ON emergency_management.distribution_plans (zone_id);
CREATE INDEX idx_distribution_plans_status ON emergency_management.distribution_plans (status);

COMMENT ON TABLE emergency_management.distribution_plans IS 'Aggregate Root DistributionPlan. No plan becomes APPROVED without an authority and a date (TS-C01). The composite FK (zone_id, emergency_id) prevents linking a plan to a zone of another emergency.';
COMMENT ON COLUMN emergency_management.distribution_plans.approved_by IS 'AuthorityId from Identity Access. Logical reference: not a foreign key because the data lives in another schema.';

-- ---------------------------------------------------------
-- distribution_items (Entity inside the DistributionPlan aggregate)
-- ---------------------------------------------------------
CREATE TABLE emergency_management.distribution_items (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    distribution_plan_id    UUID NOT NULL
        REFERENCES emergency_management.distribution_plans (id) ON DELETE CASCADE,
    resource_id             UUID NOT NULL,
    quantity                INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT uq_distribution_item_resource UNIQUE (distribution_plan_id, resource_id)
);

COMMENT ON TABLE emergency_management.distribution_items IS 'Entity DistributionItem. A resource appears only once per plan.';
COMMENT ON COLUMN emergency_management.distribution_items.resource_id IS 'ResourceId from Resource Management (Shared Kernel). Logical reference: not a foreign key because the data lives in another schema.';

-- =========================================================
-- AuxIA - Bounded Context: Resource Management
-- Engine: PostgreSQL 18 (Neon)
-- One schema per bounded context inside the same database.
-- =========================================================

CREATE SCHEMA IF NOT EXISTS resource_management;

-- ---------------------------------------------------------
-- inventories (Aggregate Root: Inventory)
-- ---------------------------------------------------------
CREATE TABLE resource_management.inventories (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL,
    name                VARCHAR(150) NOT NULL,
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_inventory_organization UNIQUE (organization_id)
);

COMMENT ON TABLE resource_management.inventories IS 'Aggregate Root Inventory. One inventory per organization in the MVP.';
COMMENT ON COLUMN resource_management.inventories.organization_id IS 'OrganizationId from Identity Access. Logical reference: not a foreign key because the data lives in another schema.';

-- ---------------------------------------------------------
-- stock_items (Entity inside the Inventory aggregate)
-- ---------------------------------------------------------
CREATE TABLE resource_management.stock_items (
    resource_id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id        UUID NOT NULL
        REFERENCES resource_management.inventories (id) ON DELETE RESTRICT,
    name                VARCHAR(150) NOT NULL,
    category            VARCHAR(10) NOT NULL
        CHECK (category IN ('WATER', 'FOOD', 'SHELTER', 'HYGIENE', 'MEDICINE', 'OTHER')),
    unit                VARCHAR(10) NOT NULL
        CHECK (unit IN ('UNIT', 'KG', 'LITER', 'KIT')),
    on_hand             INTEGER NOT NULL DEFAULT 0 CHECK (on_hand >= 0),
    reserved            INTEGER NOT NULL DEFAULT 0 CHECK (reserved >= 0),
    minimum_stock       INTEGER NOT NULL DEFAULT 0 CHECK (minimum_stock >= 0),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_stock_reserved_within_on_hand CHECK (reserved <= on_hand),
    CONSTRAINT uq_stock_item_name UNIQUE (inventory_id, name),
    CONSTRAINT uq_stock_item_inventory UNIQUE (resource_id, inventory_id)
);

COMMENT ON TABLE resource_management.stock_items IS 'Entity StockItem. Available quantity = on_hand - reserved (StockLevel value object). resource_id is the ResourceId shared with Emergency Management.';

-- ---------------------------------------------------------
-- reservations (Entity inside the Inventory aggregate)
-- ---------------------------------------------------------
CREATE TABLE resource_management.reservations (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id            UUID NOT NULL
        REFERENCES resource_management.inventories (id) ON DELETE RESTRICT,
    distribution_plan_id    UUID NOT NULL,
    status                  VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'CONSUMED', 'RELEASED')),
    reserved_at             TIMESTAMP NOT NULL DEFAULT now(),
    closed_at               TIMESTAMP,
    CONSTRAINT uq_reservation_plan UNIQUE (distribution_plan_id),
    CONSTRAINT uq_reservation_inventory UNIQUE (id, inventory_id),
    CONSTRAINT chk_reservation_closed CHECK (
        (status = 'ACTIVE' AND closed_at IS NULL)
        OR (status IN ('CONSUMED', 'RELEASED') AND closed_at IS NOT NULL)
    )
);

CREATE INDEX idx_reservations_inventory_id ON resource_management.reservations (inventory_id);

COMMENT ON TABLE resource_management.reservations IS 'Entity Reservation. Provisional reservation of stock for one distribution plan; closed when consumed or released.';
COMMENT ON COLUMN resource_management.reservations.distribution_plan_id IS 'DistributionPlanId from Emergency Management. Logical reference: not a foreign key because the data lives in another schema.';

-- ---------------------------------------------------------
-- reservation_lines (Value Object ReservationLine)
-- ---------------------------------------------------------
CREATE TABLE resource_management.reservation_lines (
    reservation_id      UUID NOT NULL,
    inventory_id        UUID NOT NULL,
    resource_id         UUID NOT NULL,
    quantity            INTEGER NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (reservation_id, resource_id),
    CONSTRAINT fk_line_reservation FOREIGN KEY (reservation_id, inventory_id)
        REFERENCES resource_management.reservations (id, inventory_id) ON DELETE CASCADE,
    CONSTRAINT fk_line_stock_item FOREIGN KEY (resource_id, inventory_id)
        REFERENCES resource_management.stock_items (resource_id, inventory_id) ON DELETE RESTRICT
);

COMMENT ON TABLE resource_management.reservation_lines IS 'Value Object ReservationLine. The composite foreign keys ensure that the reserved resource belongs to the same inventory as the reservation.';

-- ---------------------------------------------------------
-- brigades (Aggregate Root: Brigade, Staff aggregate)
-- ---------------------------------------------------------
CREATE TABLE resource_management.brigades (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id     UUID NOT NULL,
    name                VARCHAR(150) NOT NULL,
    base_latitude       DOUBLE PRECISION NOT NULL CHECK (base_latitude BETWEEN -90 AND 90),
    base_longitude      DOUBLE PRECISION NOT NULL CHECK (base_longitude BETWEEN -180 AND 180),
    status              VARCHAR(10) NOT NULL DEFAULT 'AVAILABLE'
        CHECK (status IN ('AVAILABLE', 'ASSIGNED', 'OFF_DUTY')),
    created_at          TIMESTAMP NOT NULL DEFAULT now(),
    updated_at          TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_brigade_name UNIQUE (organization_id, name)
);

CREATE INDEX idx_brigades_organization_status ON resource_management.brigades (organization_id, status);

COMMENT ON TABLE resource_management.brigades IS 'Aggregate Root Brigade. Implements the Staff aggregate: the unit assigned to a delivery is the brigade.';
COMMENT ON COLUMN resource_management.brigades.organization_id IS 'OrganizationId from Identity Access. Logical reference: not a foreign key because the data lives in another schema.';

-- ---------------------------------------------------------
-- staff_members (Entity inside the Brigade aggregate)
-- ---------------------------------------------------------
CREATE TABLE resource_management.staff_members (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brigade_id          UUID NOT NULL
        REFERENCES resource_management.brigades (id) ON DELETE CASCADE,
    full_name           VARCHAR(150) NOT NULL,
    role                VARCHAR(10) NOT NULL
        CHECK (role IN ('LEADER', 'DRIVER', 'MEDIC', 'VOLUNTEER')),
    phone               VARCHAR(20)
);

CREATE INDEX idx_staff_members_brigade_id ON resource_management.staff_members (brigade_id);

COMMENT ON TABLE resource_management.staff_members IS 'Entity StaffMember. Member of a brigade.';

-- ---------------------------------------------------------
-- brigade_assignments (Entity inside the Brigade aggregate)
-- ---------------------------------------------------------
CREATE TABLE resource_management.brigade_assignments (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    brigade_id              UUID NOT NULL
        REFERENCES resource_management.brigades (id) ON DELETE RESTRICT,
    distribution_plan_id    UUID NOT NULL,
    zone_id                 UUID NOT NULL,
    status                  VARCHAR(15) NOT NULL DEFAULT 'IN_PROGRESS'
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED')),
    assigned_at             TIMESTAMP NOT NULL DEFAULT now(),
    completed_at            TIMESTAMP,
    CONSTRAINT uq_assignment_plan UNIQUE (distribution_plan_id),
    CONSTRAINT chk_assignment_completed CHECK (
        (status = 'IN_PROGRESS' AND completed_at IS NULL)
        OR (status = 'COMPLETED' AND completed_at IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uq_brigade_active_assignment
    ON resource_management.brigade_assignments (brigade_id)
    WHERE status = 'IN_PROGRESS';

COMMENT ON TABLE resource_management.brigade_assignments IS 'Entity BrigadeAssignment. A brigade has at most one assignment in progress, and each distribution plan is assigned to one brigade.';
COMMENT ON COLUMN resource_management.brigade_assignments.distribution_plan_id IS 'DistributionPlanId from Emergency Management. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN resource_management.brigade_assignments.zone_id IS 'ZoneId from Emergency Management. Logical reference: not a foreign key because the data lives in another schema.';

-- =========================================================
-- AuxIA - Bounded Context: Traceability
-- Engine: PostgreSQL 18 (Neon)
-- One schema per bounded context inside the same database.
-- =========================================================

CREATE SCHEMA IF NOT EXISTS traceability;

-- ---------------------------------------------------------
-- deliveries (Aggregate Root: Delivery)
-- ---------------------------------------------------------
CREATE TABLE traceability.deliveries (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    distribution_plan_id    UUID NOT NULL,
    zone_id                 UUID NOT NULL,
    brigade_id              UUID NOT NULL,
    status                  VARCHAR(10) NOT NULL DEFAULT 'INITIATED'
        CHECK (status IN ('INITIATED', 'REGISTERED', 'CERTIFIED')),
    registered_by           UUID,
    delivered_at            TIMESTAMP,
    received_at             TIMESTAMP,
    record_hash             CHAR(64) CHECK (record_hash ~ '^[0-9a-f]{64}$'),
    anchor_status           VARCHAR(10)
        CHECK (anchor_status IN ('PENDING', 'ANCHORED', 'FAILED')),
    transaction_hash        CHAR(66) CHECK (transaction_hash ~ '^0x[0-9a-f]{64}$'),
    anchored_at             TIMESTAMP,
    anchor_attempts         INTEGER NOT NULL DEFAULT 0 CHECK (anchor_attempts >= 0),
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_delivery_plan UNIQUE (distribution_plan_id),
    CONSTRAINT chk_delivery_lifecycle CHECK (
        (status = 'INITIATED'
            AND registered_by IS NULL AND delivered_at IS NULL AND received_at IS NULL
            AND record_hash IS NULL AND anchor_status IS NULL
            AND transaction_hash IS NULL AND anchored_at IS NULL)
        OR (status = 'REGISTERED'
            AND registered_by IS NOT NULL AND delivered_at IS NOT NULL AND received_at IS NOT NULL
            AND record_hash IS NOT NULL AND anchor_status IN ('PENDING', 'FAILED')
            AND transaction_hash IS NULL AND anchored_at IS NULL)
        OR (status = 'CERTIFIED'
            AND registered_by IS NOT NULL AND delivered_at IS NOT NULL AND received_at IS NOT NULL
            AND record_hash IS NOT NULL AND anchor_status = 'ANCHORED'
            AND transaction_hash IS NOT NULL AND anchored_at IS NOT NULL)
    )
);

CREATE INDEX idx_deliveries_zone_id ON traceability.deliveries (zone_id);
CREATE INDEX idx_deliveries_pending_anchoring ON traceability.deliveries (anchor_status)
    WHERE anchor_status IN ('PENDING', 'FAILED');

COMMENT ON TABLE traceability.deliveries IS 'Aggregate Root Delivery. Stores the full operational record; only record_hash is sent to the blockchain (TS-C02, TS-C03).';
COMMENT ON COLUMN traceability.deliveries.record_hash IS 'SHA-256 of the canonical delivery record, which contains no personal data.';
COMMENT ON COLUMN traceability.deliveries.transaction_hash IS 'Blockchain transaction that stored record_hash.';
COMMENT ON COLUMN traceability.deliveries.distribution_plan_id IS 'DistributionPlanId from Emergency Management. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN traceability.deliveries.zone_id IS 'ZoneId from Emergency Management. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN traceability.deliveries.brigade_id IS 'BrigadeId from Resource Management. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN traceability.deliveries.registered_by IS 'UserId from Identity Access. Logical reference: not a foreign key because the data lives in another schema.';

-- ---------------------------------------------------------
-- delivery_items (Entity inside the Delivery aggregate)
-- ---------------------------------------------------------
CREATE TABLE traceability.delivery_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id     UUID NOT NULL
        REFERENCES traceability.deliveries (id) ON DELETE CASCADE,
    resource_id     UUID NOT NULL,
    quantity        INTEGER NOT NULL CHECK (quantity > 0),
    CONSTRAINT uq_delivery_item_resource UNIQUE (delivery_id, resource_id)
);

COMMENT ON TABLE traceability.delivery_items IS 'Entity DeliveryItem. Quantity actually delivered of each resource.';
COMMENT ON COLUMN traceability.delivery_items.resource_id IS 'ResourceId from Resource Management. Logical reference: not a foreign key because the data lives in another schema.';

-- ---------------------------------------------------------
-- evidences (Entity inside the Delivery aggregate)
-- ---------------------------------------------------------
CREATE TABLE traceability.evidences (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    delivery_id     UUID NOT NULL
        REFERENCES traceability.deliveries (id) ON DELETE CASCADE,
    type            VARCHAR(10) NOT NULL CHECK (type IN ('PHOTO', 'DOCUMENT')),
    storage_url     VARCHAR(500) NOT NULL,
    content_hash    CHAR(64) NOT NULL CHECK (content_hash ~ '^[0-9a-f]{64}$'),
    captured_at     TIMESTAMP NOT NULL,
    latitude        DOUBLE PRECISION CHECK (latitude BETWEEN -90 AND 90),
    longitude       DOUBLE PRECISION CHECK (longitude BETWEEN -180 AND 180),
    CONSTRAINT uq_evidence_content UNIQUE (delivery_id, content_hash),
    CONSTRAINT chk_evidence_location CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX idx_evidences_delivery_id ON traceability.evidences (delivery_id);

COMMENT ON TABLE traceability.evidences IS 'Entity Evidence. The file lives in Azure Blob Storage; content_hash is its SHA-256 and is part of the canonical record.';

-- =========================================================
-- AuxIA - Bounded Context: Identity Access
-- Engine: PostgreSQL 18 (Neon)
-- One schema per bounded context inside the same database.
-- =========================================================

CREATE SCHEMA IF NOT EXISTS identity_access;

-- ---------------------------------------------------------
-- organizations (Aggregate Root: Organization)
-- ---------------------------------------------------------
CREATE TABLE identity_access.organizations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(150) NOT NULL,
    type            VARCHAR(12) NOT NULL
        CHECK (type IN ('GOVERNMENT', 'NGO', 'PRIVATE')),
    tax_id          CHAR(11) NOT NULL CHECK (tax_id ~ '^[0-9]{11}$'),
    status          VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'SUSPENDED')),
    created_at      TIMESTAMP NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_organization_tax_id UNIQUE (tax_id)
);

COMMENT ON TABLE identity_access.organizations IS 'Aggregate Root Organization. Users of a suspended organization cannot sign in or pass role checks.';
COMMENT ON COLUMN identity_access.organizations.tax_id IS 'Peruvian taxpayer number (RUC), 11 digits.';

-- ---------------------------------------------------------
-- user_accounts (Aggregate Root: UserAccount)
-- ---------------------------------------------------------
CREATE TABLE identity_access.user_accounts (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id         UUID NOT NULL
        REFERENCES identity_access.organizations (id) ON DELETE RESTRICT,
    email                   VARCHAR(254) NOT NULL
        CHECK (email = lower(email) AND email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
    password_hash           VARCHAR(100) NOT NULL,
    hash_algorithm          VARCHAR(10) NOT NULL DEFAULT 'BCRYPT',
    first_name              VARCHAR(100) NOT NULL,
    last_name               VARCHAR(100) NOT NULL,
    role                    VARCHAR(20) NOT NULL
        CHECK (role IN ('ORGANIZATION_ADMIN', 'AUTHORITY', 'FIELD_BRIGADE', 'AUDITOR')),
    status                  VARCHAR(10) NOT NULL DEFAULT 'ACTIVE'
        CHECK (status IN ('ACTIVE', 'LOCKED', 'DISABLED')),
    failed_login_attempts   INTEGER NOT NULL DEFAULT 0
        CHECK (failed_login_attempts BETWEEN 0 AND 5),
    last_login_at           TIMESTAMP,
    created_at              TIMESTAMP NOT NULL DEFAULT now(),
    updated_at              TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_email UNIQUE (email),
    CONSTRAINT chk_user_locked CHECK (status <> 'LOCKED' OR failed_login_attempts = 5)
);

CREATE INDEX idx_user_accounts_organization_id ON identity_access.user_accounts (organization_id);

COMMENT ON TABLE identity_access.user_accounts IS 'Aggregate Root UserAccount. Each user belongs to exactly one organization; the account locks after 5 consecutive failed sign-ins.';
COMMENT ON COLUMN identity_access.user_accounts.email IS 'Stored in lowercase and unique across the platform, so sign-in does not need the organization.';
COMMENT ON COLUMN identity_access.user_accounts.password_hash IS 'BCrypt hash. The plain password is never stored.';

-- =========================================================
-- AuxIA - Bounded Context: Citizen Transparency
-- Engine: PostgreSQL 18 (Neon)
-- One schema per bounded context inside the same database.
-- Read model: tables are filled by projections of events
-- from Emergency Management and Traceability.
-- =========================================================

CREATE SCHEMA IF NOT EXISTS citizen_transparency;

-- ---------------------------------------------------------
-- emergency_public_views (Read Model: EmergencyPublicView)
-- ---------------------------------------------------------
CREATE TABLE citizen_transparency.emergency_public_views (
    emergency_id    UUID PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    department      VARCHAR(100) NOT NULL,
    province        VARCHAR(100) NOT NULL,
    district        VARCHAR(100) NOT NULL,
    start_date      DATE         NOT NULL,
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_emergency_views_district ON citizen_transparency.emergency_public_views (lower(district));

COMMENT ON TABLE citizen_transparency.emergency_public_views IS 'Read Model EmergencyPublicView. Projection of EmergencyRegistered; emergency_id is the same identifier used in Emergency Management.';

-- ---------------------------------------------------------
-- zone_public_statuses (Read Model: ZonePublicStatus)
-- ---------------------------------------------------------
CREATE TABLE citizen_transparency.zone_public_statuses (
    zone_id                         UUID PRIMARY KEY,
    emergency_id                    UUID NOT NULL
        REFERENCES citizen_transparency.emergency_public_views (emergency_id) ON DELETE RESTRICT,
    zone_name                       VARCHAR(150) NOT NULL,
    stage                           VARCHAR(15) NOT NULL DEFAULT 'REGISTERED'
        CHECK (stage IN ('REGISTERED', 'PRIORITIZED', 'AID_APPROVED', 'AID_IN_TRANSIT', 'AID_DELIVERED')),
    current_distribution_plan_id    UUID,
    current_delivery_id             UUID,
    deliveries_completed            INTEGER NOT NULL DEFAULT 0 CHECK (deliveries_completed >= 0),
    last_delivery_at                TIMESTAMP,
    last_delivery_verified          BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at                      TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_zone_stage_plan CHECK (
        stage NOT IN ('AID_APPROVED', 'AID_IN_TRANSIT', 'AID_DELIVERED')
        OR current_distribution_plan_id IS NOT NULL
    ),
    CONSTRAINT chk_zone_stage_delivery CHECK (
        stage NOT IN ('AID_IN_TRANSIT', 'AID_DELIVERED')
        OR current_delivery_id IS NOT NULL
    ),
    CONSTRAINT chk_zone_delivery_history CHECK (
        (deliveries_completed = 0 AND last_delivery_at IS NULL AND last_delivery_verified = FALSE)
        OR (deliveries_completed > 0 AND last_delivery_at IS NOT NULL)
    )
);

CREATE INDEX idx_zone_statuses_emergency_id ON citizen_transparency.zone_public_statuses (emergency_id);
CREATE INDEX idx_zone_statuses_plan_id ON citizen_transparency.zone_public_statuses (current_distribution_plan_id);
CREATE INDEX idx_zone_statuses_delivery_id ON citizen_transparency.zone_public_statuses (current_delivery_id);
CREATE INDEX idx_zone_statuses_name ON citizen_transparency.zone_public_statuses (lower(zone_name));

COMMENT ON TABLE citizen_transparency.zone_public_statuses IS 'Read Model ZonePublicStatus. Public stage of each zone. Stores no personal data, no evidence and no urgency score (TS-C02, R-04).';
COMMENT ON COLUMN citizen_transparency.zone_public_statuses.zone_id IS 'ZoneId from Emergency Management. Same identifier, copied by the projection.';
COMMENT ON COLUMN citizen_transparency.zone_public_statuses.current_distribution_plan_id IS 'DistributionPlanId from Emergency Management. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN citizen_transparency.zone_public_statuses.current_delivery_id IS 'DeliveryId from Traceability, used to build the public verification link.';
