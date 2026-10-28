package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Resource of Resource Management, referenced by its identifier. */
public record ResourceId(UUID value) {

  public ResourceId {
    Objects.requireNonNull(value, "value");
  }
}
