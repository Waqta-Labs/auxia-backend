package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.GenerateDistributionRecommendationCommandHandler.ExclusionReason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** A zone left out of the recommendation. */
public record ExcludedZoneResource(
    UUID zoneId,
    String zoneName,
    @Schema(
            description =
                "NO_SCORE: no urgency score yet; NO_NEEDS: no field report mentions a need")
        ExclusionReason reason) {}
