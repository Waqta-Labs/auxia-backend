package com.waqtalabs.auxia.traceability.domain.events;

import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;

/** The delivery of a plan is open: its brigade is on the way. */
public record DeliveryInitiated(
    DeliveryId deliveryId,
    DistributionPlanId distributionPlanId,
    ZoneId zoneId,
    BrigadeId brigadeId)
    implements DomainEvent {}
