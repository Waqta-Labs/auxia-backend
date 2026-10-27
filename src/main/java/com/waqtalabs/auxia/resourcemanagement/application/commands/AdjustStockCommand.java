package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;

/**
 * Manual correction of the physical stock, with the reason that justifies it.
 *
 * @param quantityChange positive to add, negative to remove
 */
public record AdjustStockCommand(
    InventoryId inventoryId, ResourceId resourceId, int quantityChange, String reason) {}
