package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.List;
import java.util.Objects;

/**
 * Split of the available inventory among the prioritized zones, with the justification and the
 * version of the model that produced it (US-11).
 */
public record DistributionRecommendation(
    List<ZoneAllocation> allocations, String justification, String modelVersion) {

  public DistributionRecommendation {
    allocations = List.copyOf(allocations);
    Objects.requireNonNull(justification, "justification");
    Objects.requireNonNull(modelVersion, "modelVersion");
  }
}
