-- Deliveries (US-15 to US-18, TS-05). A delivery belongs to the organization of its
-- plan, so each organization reads only its own deliveries. No delivery was stored
-- before this version.
--
-- The transaction that anchors the hash is kept as soon as it is sent, while the
-- delivery is still REGISTERED and its anchoring PENDING. If the process stops before
-- the transaction is confirmed, the retry checks that transaction instead of sending
-- the hash again, which the contract would reject.

ALTER TABLE traceability.deliveries
    ADD COLUMN organization_id UUID NOT NULL,
    DROP CONSTRAINT chk_delivery_lifecycle,
    ADD CONSTRAINT chk_delivery_lifecycle CHECK (
        (status = 'INITIATED'
            AND registered_by IS NULL AND delivered_at IS NULL AND received_at IS NULL
            AND record_hash IS NULL AND anchor_status IS NULL
            AND transaction_hash IS NULL AND anchored_at IS NULL)
        OR (status = 'REGISTERED'
            AND registered_by IS NOT NULL AND delivered_at IS NOT NULL AND received_at IS NOT NULL
            AND record_hash IS NOT NULL AND anchored_at IS NULL
            AND (anchor_status = 'PENDING'
                OR (anchor_status = 'FAILED' AND transaction_hash IS NULL)))
        OR (status = 'CERTIFIED'
            AND registered_by IS NOT NULL AND delivered_at IS NOT NULL AND received_at IS NOT NULL
            AND record_hash IS NOT NULL AND anchor_status = 'ANCHORED'
            AND transaction_hash IS NOT NULL AND anchored_at IS NOT NULL)
    );

CREATE INDEX idx_deliveries_zone_organization
    ON traceability.deliveries (zone_id, organization_id);

COMMENT ON COLUMN traceability.deliveries.organization_id IS 'OrganizationId from Identity Access: the organization whose plan the delivery executes. Logical reference: not a foreign key because the data lives in another schema.';
COMMENT ON COLUMN traceability.deliveries.transaction_hash IS 'Blockchain transaction that carries record_hash. Kept from the moment it is sent; confirmed when the delivery is CERTIFIED.';
