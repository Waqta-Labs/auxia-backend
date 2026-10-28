package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;

/** A quantity of a resource, as recommended or as the authority sets it. */
public record ResourceQuantity(ResourceId resourceId, int quantity) {

  public ResourceQuantity {
    Objects.requireNonNull(resourceId, "resourceId");
    if (quantity < 0) {
      throw new IllegalArgumentException("quantity must not be negative");
    }
  }
}
