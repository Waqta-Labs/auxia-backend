package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.List;
import java.util.Objects;

/** What a recommendation assigns to one zone and how much of its need that covers. */
public record ZoneAllocation(ZoneId zoneId, List<ResourceQuantity> items, Coverage coverage) {

  public ZoneAllocation {
    Objects.requireNonNull(zoneId, "zoneId");
    Objects.requireNonNull(coverage, "coverage");
    items = items.stream().filter(item -> item.quantity() > 0).toList();
  }

  /** Whether the zone receives something, so it gets a distribution plan. */
  public boolean receivesResources() {
    return !items.isEmpty();
  }
}
