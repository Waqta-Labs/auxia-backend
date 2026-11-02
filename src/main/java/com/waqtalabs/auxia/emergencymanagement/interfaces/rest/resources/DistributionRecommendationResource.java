package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Result of a recommendation: the proposed plans and what it could not cover (US-11, scenario 2).
 */
public record DistributionRecommendationResource(
    @Schema(description = "One proposed plan for each zone that receives resources")
        List<DistributionPlanResource> plans,
    @Schema(description = "Zones left partially or totally unattended for lack of inventory")
        List<UnattendedZoneResource> unattendedZones,
    @Schema(description = "Zones of the emergency that the recommendation did not consider")
        List<ExcludedZoneResource> excludedZones,
    String justification,
    String modelVersion) {}
