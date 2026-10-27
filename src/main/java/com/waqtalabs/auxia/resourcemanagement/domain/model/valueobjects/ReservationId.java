package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a reservation of stock. */
public record ReservationId(UUID value) {

  public ReservationId {
    Objects.requireNonNull(value, "value");
  }

  public static ReservationId generate() {
    return new ReservationId(UUID.randomUUID());
  }
}
