package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Zone of Emergency Management, referenced by its identifier. */
public record ZoneId(UUID value) {

  public ZoneId {
    Objects.requireNonNull(value, "value");
  }
}
