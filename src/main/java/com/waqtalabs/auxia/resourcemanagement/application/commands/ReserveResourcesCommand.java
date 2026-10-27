package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import java.util.List;

/** Emergency Management holds the stock of a proposed distribution plan. */
public record ReserveResourcesCommand(
    OrganizationId organizationId,
    DistributionPlanId distributionPlanId,
    List<ReservationLine> lines) {}
