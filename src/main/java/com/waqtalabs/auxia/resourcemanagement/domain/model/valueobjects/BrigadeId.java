package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a brigade. */
public record BrigadeId(UUID value) {

  public BrigadeId {
    Objects.requireNonNull(value, "value");
  }

  public static BrigadeId generate() {
    return new BrigadeId(UUID.randomUUID());
  }
}
