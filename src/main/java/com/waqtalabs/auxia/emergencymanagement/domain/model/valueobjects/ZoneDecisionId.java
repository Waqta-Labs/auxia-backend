package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an entry of the history of a zone. */
public record ZoneDecisionId(UUID value) {

  public ZoneDecisionId {
    Objects.requireNonNull(value, "value");
  }

  public static ZoneDecisionId generate() {
    return new ZoneDecisionId(UUID.randomUUID());
  }
}
