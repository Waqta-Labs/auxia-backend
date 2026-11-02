package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a field report. */
public record FieldReportId(UUID value) {

  public FieldReportId {
    Objects.requireNonNull(value, "value");
  }

  public static FieldReportId generate() {
    return new FieldReportId(UUID.randomUUID());
  }
}
