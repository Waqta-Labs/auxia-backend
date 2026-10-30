package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;

/** Compares each resource of an inventory with its minimum stock. */
public record EvaluateStockAlertsCommand(InventoryId inventoryId) {}
