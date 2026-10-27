package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;

/**
 * An organization adds a resource to its inventory, without stock.
 *
 * @param minimumStock available quantity below which the resource needs restocking
 */
public record RegisterResourceCommand(
    InventoryId inventoryId,
    OrganizationId organizationId,
    String name,
    ResourceCategory category,
    UnitOfMeasure unit,
    int minimumStock) {}
