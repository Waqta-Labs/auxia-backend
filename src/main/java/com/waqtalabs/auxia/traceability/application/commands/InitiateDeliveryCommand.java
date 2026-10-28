package com.waqtalabs.auxia.traceability.application.commands;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;

/** Opens the delivery of a plan whose brigade was just assigned. */
public record InitiateDeliveryCommand(
    DistributionPlanId distributionPlanId,
    ZoneId zoneId,
    BrigadeId brigadeId,
    OrganizationId organizationId) {}
