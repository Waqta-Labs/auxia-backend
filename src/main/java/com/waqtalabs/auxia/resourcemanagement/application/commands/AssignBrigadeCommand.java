package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import java.util.Optional;

/**
 * Assigns a brigade of the organization to an approved distribution plan.
 *
 * @param zoneLocation where the zone is; empty when it was registered without coordinates
 */
public record AssignBrigadeCommand(
    OrganizationId organizationId,
    DistributionPlanId distributionPlanId,
    ZoneId zoneId,
    Optional<Location> zoneLocation) {}
