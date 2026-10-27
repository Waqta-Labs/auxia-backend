package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Schema(
    description =
        "Urgency score of a zone with its explanation (TS-02, US-05). When available is false,"
            + " the score fields are null and missingVariables lists what the zone lacks.")
public record ZoneScoreResource(
    UUID zoneId,
    boolean available,
    @Schema(example = "54.9") Double score,
    PriorityLevel priorityLevel,
    @Schema(description = "Variables that weighed the most, from the highest contribution")
        List<ScoreContributionResource> topFeatures,
    @Schema(example = "rules-v1") String modelVersion,
    LocalDateTime calculatedAt,
    @Schema(description = "Empty when the score is available", example = "[\"population\"]")
        List<String> missingVariables) {}
