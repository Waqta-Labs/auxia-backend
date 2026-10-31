package com.waqtalabs.auxia.traceability.interfaces.acl;

import java.util.UUID;

/** Public version of {@code DeliveryCertified}: the hash of the delivery is in the blockchain. */
public record DeliveryCertifiedIntegrationEvent(UUID deliveryId, UUID zoneId) {}
