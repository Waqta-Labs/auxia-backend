package com.waqtalabs.auxia.traceability.domain.events;

import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;

/** The hash of a delivery is anchored in the blockchain. */
public record DeliveryCertified(
    DeliveryId deliveryId,
    ZoneId zoneId,
    String transactionHash)
    implements DomainEvent {}
