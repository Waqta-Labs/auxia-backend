package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import java.time.LocalDateTime;
import java.util.UUID;

/** The distribution plan that a brigade is delivering. */
public record BrigadeAssignmentResource(
    UUID distributionPlanId, UUID zoneId, LocalDateTime assignedAt) {}
