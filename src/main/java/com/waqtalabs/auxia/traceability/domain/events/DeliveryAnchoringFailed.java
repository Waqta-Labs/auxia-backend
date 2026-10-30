package com.waqtalabs.auxia.traceability.domain.events;

import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;

/** The blockchain rejected the hash of a delivery or did not answer; it will be sent again. */
public record DeliveryAnchoringFailed(
    DeliveryId deliveryId,
    int attempts)
    implements DomainEvent {}
