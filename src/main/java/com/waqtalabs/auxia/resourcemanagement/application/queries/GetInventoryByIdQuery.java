package com.waqtalabs.auxia.resourcemanagement.application.queries;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;

/** An inventory of the organization of the user; one of another organization is not found. */
public record GetInventoryByIdQuery(InventoryId inventoryId, OrganizationId organizationId) {}
