package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Public version of {@code BrigadeAssigned}: a brigade will deliver a distribution plan.
 * Traceability opens the delivery with it. It carries only identifiers, so no other context
 * depends on the domain of Resource Management.
 */
public record BrigadeAssignedIntegrationEvent(
    UUID brigadeId,
    UUID distributionPlanId,
    UUID zoneId,
    UUID organizationId,
    LocalDateTime assignedAt) {}
