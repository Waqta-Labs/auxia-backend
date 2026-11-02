package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/**
 * Identifier of a resource of an inventory. Emergency Management uses the same
 * value in each line of a distribution plan (Shared Kernel).
 */
public record ResourceId(UUID value) {

  public ResourceId {
    Objects.requireNonNull(value, "value");
  }

  public static ResourceId generate() {
    return new ResourceId(UUID.randomUUID());
  }
}
