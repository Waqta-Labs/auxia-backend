package com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Delivery of Traceability. It is also its public verification code (US-26). */
public record DeliveryId(UUID value) {

  public DeliveryId {
    Objects.requireNonNull(value, "value");
  }
}
