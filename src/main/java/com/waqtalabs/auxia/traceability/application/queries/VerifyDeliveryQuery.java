package com.waqtalabs.auxia.traceability.application.queries;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;

/**
 * Whether a registered delivery is still the one anchored in the blockchain (US-17). Anyone can
 * ask, with the identifier of the delivery as its public code.
 */
public record VerifyDeliveryQuery(DeliveryId deliveryId) {}
