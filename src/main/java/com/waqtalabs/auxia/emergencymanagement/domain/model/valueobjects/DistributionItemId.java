package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a line of a distribution plan. */
public record DistributionItemId(UUID value) {

  public DistributionItemId {
    Objects.requireNonNull(value, "value");
  }

  public static DistributionItemId generate() {
    return new DistributionItemId(UUID.randomUUID());
  }
}
