package com.waqtalabs.auxia.traceability.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.UUID;

/** Quantity of a resource of the plan that the brigade delivered. */
public record DeliveredItemRequest(@NotNull UUID resourceId, @NotNull @Positive Integer quantity) {}
