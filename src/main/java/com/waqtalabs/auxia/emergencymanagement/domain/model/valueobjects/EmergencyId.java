package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an emergency. */
public record EmergencyId(UUID value) {

  public EmergencyId {
    Objects.requireNonNull(value, "value");
  }

  public static EmergencyId generate() {
    return new EmergencyId(UUID.randomUUID());
  }
}
