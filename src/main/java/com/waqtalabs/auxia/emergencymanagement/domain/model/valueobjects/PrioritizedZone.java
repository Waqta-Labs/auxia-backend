package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A zone with an urgency score, as the distribution recommendation receives it.
 *
 * @param alreadyAllocated units of each category that its approved or executed plans already give
 *     it, so the recommendation only covers what it still lacks
 */
public record PrioritizedZone(
    ZoneId zoneId,
    double score,
    int peopleAffected,
    Set<Need> needs,
    Map<Need, Integer> alreadyAllocated) {

  public PrioritizedZone {
    Objects.requireNonNull(zoneId, "zoneId");
    needs = Set.copyOf(needs);
    alreadyAllocated = Map.copyOf(alreadyAllocated);
  }

  /** A zone that no plan has given anything yet. */
  public PrioritizedZone(ZoneId zoneId, double score, int peopleAffected, Set<Need> needs) {
    this(zoneId, score, peopleAffected, needs, Map.of());
  }
}
