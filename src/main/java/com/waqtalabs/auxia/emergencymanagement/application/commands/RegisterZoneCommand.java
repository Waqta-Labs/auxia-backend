package com.waqtalabs.auxia.emergencymanagement.application.commands;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;

/** Registers a zone of an emergency. {@code geolocation} is optional. */
public record RegisterZoneCommand(
    EmergencyId emergencyId,
    String name,
    Geolocation geolocation,
    PopulationImpact populationImpact,
    DamageLevel damageLevel) {}
