package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * {@code score} and {@code priorityLevel} are {@code null} until the score is calculated, and
 * {@code lastAidReceivedAt} until a brigade delivers aid in the zone.
 */
public record ZoneResource(
    UUID id,
    UUID emergencyId,
    String name,
    GeolocationResource geolocation,
    PopulationImpactResource populationImpact,
    DamageLevel damageLevel,
    ZoneStatus status,
    Double score,
    PriorityLevel priorityLevel,
    LocalDateTime lastAidReceivedAt) {}
