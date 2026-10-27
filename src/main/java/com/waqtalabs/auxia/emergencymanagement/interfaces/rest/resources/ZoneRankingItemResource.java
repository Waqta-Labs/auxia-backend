package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import java.util.UUID;

@Schema(description = "One zone of the urgency ranking (US-06)")
public record ZoneRankingItemResource(
    UUID id,
    String name,
    ZoneStatus status,
    DamageLevel damageLevel,
    int peopleAffected,
    @Schema(description = "Null while the score is not available", example = "54.9")
        Double score,
    PriorityLevel priorityLevel,
    @Schema(description = "Needs mentioned in the processed field reports of the zone")
        Set<Need> needs) {}
