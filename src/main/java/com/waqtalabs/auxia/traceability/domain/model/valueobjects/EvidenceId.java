package com.waqtalabs.auxia.traceability.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of an evidence file. */
public record EvidenceId(UUID value) {

  public EvidenceId {
    Objects.requireNonNull(value, "value");
  }

  public static EvidenceId generate() {
    return new EvidenceId(UUID.randomUUID());
  }
}
