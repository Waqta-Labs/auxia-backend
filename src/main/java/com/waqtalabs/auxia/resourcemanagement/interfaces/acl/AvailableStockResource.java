package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import java.util.UUID;

/**
 * Available quantity of one resource, as other bounded contexts see it.
 *
 * @param category name of the resource category, such as {@code WATER}
 * @param unit name of the unit of measure, such as {@code KIT}
 */
public record AvailableStockResource(
    UUID resourceId, String name, String category, String unit, int available) {}
