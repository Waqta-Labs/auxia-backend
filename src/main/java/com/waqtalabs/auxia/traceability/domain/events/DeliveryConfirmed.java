package com.waqtalabs.auxia.traceability.domain.events;

import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.CanonicalDeliveryRecord;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.time.LocalDateTime;
import java.util.List;

/**
 * A delivery was registered with its evidence. Resource Management consumes its stock and frees
 * the brigade; Emergency Management marks the plan executed and the zone served.
 */
public record DeliveryConfirmed(
    DeliveryId deliveryId,
    DistributionPlanId distributionPlanId,
    ZoneId zoneId,
    BrigadeId brigadeId,
    OrganizationId organizationId,
    List<CanonicalDeliveryRecord.Line> items,
    LocalDateTime deliveredAt)
    implements DomainEvent {}
