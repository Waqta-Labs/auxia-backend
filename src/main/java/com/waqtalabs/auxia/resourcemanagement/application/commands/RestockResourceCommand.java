package com.waqtalabs.auxia.resourcemanagement.application.commands;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;

/** A batch of a resource is received, such as a donation (US-08). */
public record RestockResourceCommand(
    InventoryId inventoryId, OrganizationId organizationId, ResourceId resourceId, int quantity) {}
