package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** User of Identity Access who registered a delivery, referenced by its identifier. */
public record UserId(UUID value) {

  public UserId {
    Objects.requireNonNull(value, "value");
  }
}
