package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a delivered resource line. */
public record DeliveryItemId(UUID value) {

  public DeliveryItemId {
    Objects.requireNonNull(value, "value");
  }

  public static DeliveryItemId generate() {
    return new DeliveryItemId(UUID.randomUUID());
  }
}
