package com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Reference to a distribution plan of Emergency Management. */
public record DistributionPlanId(UUID value) {

  public DistributionPlanId {
    Objects.requireNonNull(value, "value");
  }
}
