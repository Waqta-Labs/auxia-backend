package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

/**
 * Changes some counts of the population impact of a zone. A {@code null} count keeps its current
 * value.
 */
public record UpdateZonePopulationImpactCommand(
    ZoneId zoneId,
    Integer population,
    Integer familiesAffected,
    Integer peopleAffected,
    Integer children,
    Integer elderlyPeople,
    Integer disabledPeople) {}
