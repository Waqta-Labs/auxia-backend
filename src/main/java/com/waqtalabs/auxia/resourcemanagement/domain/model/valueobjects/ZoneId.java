package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Reference to a zone of Emergency Management. */
public record ZoneId(UUID value) {

  public ZoneId {
    Objects.requireNonNull(value, "value");
  }
}
