package com.waqtalabs.auxia.resourcemanagement.domain.events;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/**
 * The available stock of an inventory changed: a resource was registered or restocked, adjusted,
 * reserved, released or consumed. Its low stock alerts are evaluated again.
 */
public record InventoryStockChanged(InventoryId inventoryId) implements DomainEvent {}
