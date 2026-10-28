package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;

/**
 * A resource of the inventory with the quantity that can still be distributed, as Resource
 * Management reports it. The category and the unit keep the names of Resource Management.
 */
public record AvailableResource(
    ResourceId resourceId, String name, String category, String unit, int available) {

  public AvailableResource {
    Objects.requireNonNull(resourceId, "resourceId");
    Objects.requireNonNull(category, "category");
    Objects.requireNonNull(unit, "unit");
  }
}
