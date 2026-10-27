package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import java.util.List;
import java.util.UUID;

/** {@code stockItems} are ordered by name. */
public record InventoryResource(
    UUID id, UUID organizationId, String name, List<StockItemResource> stockItems) {}
