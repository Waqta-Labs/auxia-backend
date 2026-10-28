package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import java.util.UUID;

/** A zone that the recommendation covers only in part (PARTIAL) or not at all (NONE). */
public record UnattendedZoneResource(UUID zoneId, String zoneName, Coverage coverage) {}
