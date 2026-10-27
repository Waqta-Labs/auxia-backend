package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an inventory. */
public record InventoryId(UUID value) {

  public InventoryId {
    Objects.requireNonNull(value, "value");
  }

  public static InventoryId generate() {
    return new InventoryId(UUID.randomUUID());
  }
}
