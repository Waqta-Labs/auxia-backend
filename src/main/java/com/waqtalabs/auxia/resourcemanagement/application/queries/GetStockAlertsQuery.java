package com.waqtalabs.auxia.resourcemanagement.application.queries;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;

/**
 * Alerts of an inventory of the organization of the user.
 *
 * @param status {@code null} for every alert
 */
public record GetStockAlertsQuery(
    InventoryId inventoryId, OrganizationId organizationId, StockAlertStatus status) {}
