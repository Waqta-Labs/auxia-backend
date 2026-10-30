package com.waqtalabs.auxia.traceability.application.commands;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryId;

/** Anchors the hash of a registered delivery in the blockchain (US-16). */
public record AnchorDeliveryCommand(DeliveryId deliveryId) {}
