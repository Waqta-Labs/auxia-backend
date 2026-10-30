package com.waqtalabs.auxia.resourcemanagement.domain.events;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import java.time.LocalDateTime;

/** A brigade will deliver a distribution plan. Traceability opens the delivery with it. */
public record BrigadeAssigned(
    BrigadeId brigadeId,
    DistributionPlanId distributionPlanId,
    ZoneId zoneId,
    OrganizationId organizationId,
    LocalDateTime assignedAt)
    implements DomainEvent {}
