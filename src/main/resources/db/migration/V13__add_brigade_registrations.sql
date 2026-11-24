-- Registration of brigadists from the mobile app (A_02). Each brigade has a code, unique in the
-- platform, that the brigadist writes; the existing brigades get one from their id. A request
-- waits for an ORGANIZATION_ADMIN of the organization of that brigade, who approves it (a
-- FIELD_BRIGADE account is created and added to the brigade) or rejects it. The phone generates
-- the id of the request and a secret, stored as a hash, which is the credential of the account.

ALTER TABLE resource_management.brigades
    ADD COLUMN code VARCHAR(30);

UPDATE resource_management.brigades
SET code = 'BRG-' || upper(substr(replace(id::text, '-', ''), 1, 8));

ALTER TABLE resource_management.brigades
    ALTER COLUMN code SET NOT NULL,
    ADD CONSTRAINT uq_brigade_code UNIQUE (code),
    ADD CONSTRAINT chk_brigade_code CHECK (code ~ '^[A-Z0-9-]{3,30}$');

CREATE TABLE identity_access.brigade_registrations (
    id                    UUID PRIMARY KEY,
    full_name             VARCHAR(150) NOT NULL,
    phone                 VARCHAR(20) NOT NULL,
    organization_name     VARCHAR(150) NOT NULL,
    brigade_code          VARCHAR(30) NOT NULL,
    brigade_id            UUID NOT NULL,
    organization_id       UUID NOT NULL,
    device_secret_hash    VARCHAR(100) NOT NULL,
    device_secret_algorithm VARCHAR(20) NOT NULL,
    id_card_url           TEXT,
    id_card_content_type  VARCHAR(50),
    status                VARCHAR(10) NOT NULL
        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    rejection_reason      TEXT,
    submitted_at          TIMESTAMP NOT NULL,
    decided_by            UUID,
    decided_at            TIMESTAMP,
    user_id               UUID,
    CONSTRAINT chk_brigade_registration_decision CHECK (
        (status = 'PENDING') = (decided_at IS NULL)
        AND (status = 'APPROVED') = (user_id IS NOT NULL)
        AND (status = 'REJECTED') = (rejection_reason IS NOT NULL))
);

CREATE INDEX idx_brigade_registrations_organization
    ON identity_access.brigade_registrations (organization_id, status);

COMMENT ON TABLE identity_access.brigade_registrations IS 'Aggregate BrigadeRegistration. Request of a brigadist from the mobile app (A_02); brigade_id and organization_id are logical references to resource_management.';
