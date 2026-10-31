package com.waqtalabs.auxia.traceability.interfaces.acl;

import java.util.UUID;

/** Public version of {@code DeliveryInitiated}: the brigade of a plan is on its way. */
public record DeliveryInitiatedIntegrationEvent(
    UUID deliveryId, UUID distributionPlanId, UUID zoneId) {}
