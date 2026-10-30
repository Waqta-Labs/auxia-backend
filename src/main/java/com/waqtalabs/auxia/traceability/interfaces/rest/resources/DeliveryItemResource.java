package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import java.util.UUID;

/** Quantity of a resource that was delivered. */
public record DeliveryItemResource(UUID resourceId, int quantity) {}
