package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

/**
 * Variables of a zone from which its urgency score is calculated. They are built by {@link
 * com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone#urgencyIndicators}.
 *
 * @param population people living in the zone
 */
public record UrgencyIndicators(
    DamageLevel damageLevel,
    int population,
    int familiesAffected,
    int peopleAffected,
    int children,
    int elderlyPeople,
    int disabledPeople,
    Set<Need> needs) {

  public UrgencyIndicators {
    Objects.requireNonNull(damageLevel, "damageLevel");
    if (population <= 0) {
      throw new IllegalArgumentException("population must be positive");
    }
    if (familiesAffected < 0
        || peopleAffected < 0
        || children < 0
        || elderlyPeople < 0
        || disabledPeople < 0) {
      throw new IllegalArgumentException("counts must not be negative");
    }
    needs =
        needs == null || needs.isEmpty()
            ? Set.of()
            : Collections.unmodifiableSet(EnumSet.copyOf(needs));
  }
}
