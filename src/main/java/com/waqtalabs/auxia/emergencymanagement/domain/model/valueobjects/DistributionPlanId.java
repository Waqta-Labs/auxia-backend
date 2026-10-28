package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Identifier of a distribution plan. */
public record DistributionPlanId(UUID value) {

  public DistributionPlanId {
    Objects.requireNonNull(value, "value");
  }

  public static DistributionPlanId generate() {
    return new DistributionPlanId(UUID.randomUUID());
  }
}
