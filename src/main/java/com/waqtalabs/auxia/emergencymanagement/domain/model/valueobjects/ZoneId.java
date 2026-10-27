package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a zone. */
public record ZoneId(UUID value) {

  public ZoneId {
    Objects.requireNonNull(value, "value");
  }

  public static ZoneId generate() {
    return new ZoneId(UUID.randomUUID());
  }
}
