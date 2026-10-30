package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Brigade of Resource Management, referenced by its identifier. */
public record BrigadeId(UUID value) {

  public BrigadeId {
    Objects.requireNonNull(value, "value");
  }
}
