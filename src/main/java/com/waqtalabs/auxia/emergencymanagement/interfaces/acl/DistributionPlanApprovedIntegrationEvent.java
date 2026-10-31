package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import java.util.UUID;

/** Public version of {@code DistributionPlanApproved}: aid for the zone is approved. */
public record DistributionPlanApprovedIntegrationEvent(UUID distributionPlanId, UUID zoneId) {}
