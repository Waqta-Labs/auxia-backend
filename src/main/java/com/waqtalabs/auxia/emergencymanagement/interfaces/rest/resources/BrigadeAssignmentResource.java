package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources;

import java.util.UUID;

/** The brigade that will deliver an approved plan. */
public record BrigadeAssignmentResource(UUID distributionPlanId, UUID brigadeId) {}
