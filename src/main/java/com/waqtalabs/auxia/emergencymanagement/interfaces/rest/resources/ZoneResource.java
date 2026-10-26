package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import java.util.UUID;

public record ZoneResource(
    UUID id,
    UUID emergencyId,
    String name,
    GeolocationResource geolocation,
    PopulationImpactResource populationImpact,
    DamageLevel damageLevel,
    ZoneStatus status) {}
