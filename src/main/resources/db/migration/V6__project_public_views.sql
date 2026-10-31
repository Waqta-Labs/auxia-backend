-- Public view (US-24 to US-26). From now on the projections of Citizen Transparency
-- keep emergency_public_views and zone_public_statuses up to date with the events of
-- Emergency Management and Traceability. This migration projects, once, what already
-- existed before those projections, so the public view does not start empty.

INSERT INTO citizen_transparency.emergency_public_views
    (emergency_id, name, type, department, province, district, start_date)
SELECT id, name, type, department, province, district, start_date
FROM emergency_management.emergencies
ON CONFLICT (emergency_id) DO NOTHING;

-- The stage of each zone follows its latest approved (or executed) plan and the
-- delivery of that plan; without a plan, whether it has an urgency score.
WITH latest_plan AS (
    SELECT DISTINCT ON (zone_id) zone_id, id AS plan_id
    FROM emergency_management.distribution_plans
    WHERE status IN ('APPROVED', 'EXECUTED')
    ORDER BY zone_id, approved_at DESC
),
plan_delivery AS (
    SELECT distribution_plan_id, id AS delivery_id, status
    FROM traceability.deliveries
),
completed AS (
    SELECT zone_id, count(*) AS deliveries, max(delivered_at) AS last_at
    FROM traceability.deliveries
    WHERE status IN ('REGISTERED', 'CERTIFIED')
    GROUP BY zone_id
),
last_completed AS (
    SELECT DISTINCT ON (zone_id) zone_id, id AS delivery_id, status
    FROM traceability.deliveries
    WHERE status IN ('REGISTERED', 'CERTIFIED')
    ORDER BY zone_id, delivered_at DESC
)
INSERT INTO citizen_transparency.zone_public_statuses
    (zone_id, emergency_id, zone_name, stage, current_distribution_plan_id,
     current_delivery_id, deliveries_completed, last_delivery_at, last_delivery_verified)
SELECT z.id,
       z.emergency_id,
       z.name,
       CASE
           WHEN pd.status IN ('REGISTERED', 'CERTIFIED') THEN 'AID_DELIVERED'
           WHEN pd.status = 'INITIATED' THEN 'AID_IN_TRANSIT'
           WHEN lp.plan_id IS NOT NULL THEN 'AID_APPROVED'
           WHEN z.urgency_score IS NOT NULL THEN 'PRIORITIZED'
           ELSE 'REGISTERED'
       END,
       lp.plan_id,
       COALESCE(pd.delivery_id, lc.delivery_id),
       COALESCE(c.deliveries, 0),
       c.last_at,
       COALESCE(lc.status = 'CERTIFIED', FALSE)
FROM emergency_management.zones z
LEFT JOIN latest_plan lp ON lp.zone_id = z.id
LEFT JOIN plan_delivery pd ON pd.distribution_plan_id = lp.plan_id
LEFT JOIN completed c ON c.zone_id = z.id
LEFT JOIN last_completed lc ON lc.zone_id = z.id
ON CONFLICT (zone_id) DO NOTHING;
