package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a delivery. It is also the public code to verify it (US-26). */
public record DeliveryId(UUID value) {

  public DeliveryId {
    Objects.requireNonNull(value, "value");
  }

  public static DeliveryId generate() {
    return new DeliveryId(UUID.randomUUID());
  }
}
