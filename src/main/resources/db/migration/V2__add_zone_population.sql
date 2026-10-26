-- Total number of people living in a zone (PopulationImpact.population).
-- The urgency score of the AI Service needs it as zonePopulation to weigh how
-- much of the zone is affected. It is optional: zones registered before this
-- column, or whose population is unknown, keep NULL.
ALTER TABLE emergency_management.zones
    ADD COLUMN population INTEGER
        CONSTRAINT chk_zone_population CHECK (population > 0);

ALTER TABLE emergency_management.zones
    ADD CONSTRAINT chk_zone_people_affected_population
        CHECK (population IS NULL OR people_affected <= population);

COMMENT ON COLUMN emergency_management.zones.population IS 'People living in the zone (PopulationImpact.population). NULL when unknown. The urgency score sends it to the AI Service as zonePopulation.';
