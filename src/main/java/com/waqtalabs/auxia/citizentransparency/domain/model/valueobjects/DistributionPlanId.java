package com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects;

import java.util.Objects;
import java.util.UUID;

/** Distribution plan of Emergency Management, referenced by its identifier. */
public record DistributionPlanId(UUID value) {

  public DistributionPlanId {
    Objects.requireNonNull(value, "value");
  }
}
