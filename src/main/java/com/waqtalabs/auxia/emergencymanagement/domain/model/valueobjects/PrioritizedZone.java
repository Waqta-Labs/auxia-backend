package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;
import java.util.Set;

/** A zone with an urgency score, as the distribution recommendation receives it. */
public record PrioritizedZone(ZoneId zoneId, double score, int peopleAffected, Set<Need> needs) {

  public PrioritizedZone {
    Objects.requireNonNull(zoneId, "zoneId");
    needs = Set.copyOf(needs);
  }
}
