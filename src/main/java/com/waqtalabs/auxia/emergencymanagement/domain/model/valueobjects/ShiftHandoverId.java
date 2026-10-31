package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a shift handover between two authorities. */
public record ShiftHandoverId(UUID value) {

  public ShiftHandoverId {
    Objects.requireNonNull(value, "value");
  }

  public static ShiftHandoverId generate() {
    return new ShiftHandoverId(UUID.randomUUID());
  }
}
