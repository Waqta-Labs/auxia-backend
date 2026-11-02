-- Low stock alerts (US-10). An alert is raised when the available quantity of a
-- resource falls below its minimum stock, and resolved when it is restocked above it.
-- A resource has one open alert at most.

CREATE TABLE resource_management.stock_alerts (
    id                  UUID PRIMARY KEY,
    inventory_id        UUID NOT NULL
        REFERENCES resource_management.inventories (id) ON DELETE CASCADE,
    resource_id         UUID NOT NULL,
    resource_name       VARCHAR(150) NOT NULL,
    available_quantity  INTEGER NOT NULL CHECK (available_quantity >= 0),
    minimum_stock       INTEGER NOT NULL CHECK (minimum_stock > 0),
    status              VARCHAR(10) NOT NULL CHECK (status IN ('OPEN', 'RESOLVED')),
    raised_at           TIMESTAMP NOT NULL,
    resolved_at         TIMESTAMP,
    CONSTRAINT chk_stock_alert_resolution CHECK ((status = 'RESOLVED') = (resolved_at IS NOT NULL)),
    CONSTRAINT chk_stock_alert_below_minimum CHECK (available_quantity < minimum_stock)
);

CREATE UNIQUE INDEX uq_stock_alert_open
    ON resource_management.stock_alerts (inventory_id, resource_id)
    WHERE status = 'OPEN';
CREATE INDEX idx_stock_alerts_inventory
    ON resource_management.stock_alerts (inventory_id, raised_at DESC);

COMMENT ON TABLE resource_management.stock_alerts IS 'Aggregate StockAlert. A resource below its minimum stock (US-10); it keeps the name and quantities of the moment it was raised.';
COMMENT ON COLUMN resource_management.stock_alerts.resource_id IS 'Resource of the inventory (stock_items.resource_id).';
