package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Schema(
    description =
        "Affected population of the zone. A missing count is 0. No vulnerable group can exceed"
            + " peopleAffected, and peopleAffected cannot exceed population.")
public record PopulationImpactResource(
    @Schema(description = "People living in the zone; optional", example = "600") @Positive
        Integer population,
    @Schema(example = "35") @PositiveOrZero Integer familiesAffected,
    @Schema(example = "140") @PositiveOrZero Integer peopleAffected,
    @Schema(example = "40") @PositiveOrZero Integer children,
    @Schema(example = "15") @PositiveOrZero Integer elderlyPeople,
    @Schema(example = "0") @PositiveOrZero Integer disabledPeople) {}
