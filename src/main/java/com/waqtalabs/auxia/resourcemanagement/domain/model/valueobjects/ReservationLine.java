package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;

/** Quantity of a resource reserved for a distribution plan. */
public record ReservationLine(ResourceId resourceId, int quantity) {

  public ReservationLine {
    Objects.requireNonNull(resourceId, "resourceId");
    if (quantity <= 0) {
      throw new IllegalArgumentException("quantity must be positive");
    }
  }
}
