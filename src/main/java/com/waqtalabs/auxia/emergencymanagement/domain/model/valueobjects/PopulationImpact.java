package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InconsistentPopulationImpactException;
import java.util.Optional;

/**
 * Affected population of a zone and its vulnerable groups.
 *
 * <p>{@code population} is the total number of people living in the zone. It is optional, but the
 * urgency score needs it to weigh how much of the zone is affected. A person can belong to more
 * than one vulnerable group, so each group is compared with {@code peopleAffected} on its own.
 *
 * @param population people living in the zone, or {@code null} when unknown
 */
public record PopulationImpact(
    Integer population,
    int familiesAffected,
    int peopleAffected,
    int children,
    int elderlyPeople,
    int disabledPeople) {

  public PopulationImpact {
    if (population != null && population <= 0) {
      throw new IllegalArgumentException("population must be positive");
    }
    requireNotNegative(familiesAffected, "familiesAffected");
    requireNotNegative(peopleAffected, "peopleAffected");
    requireNotNegative(children, "children");
    requireNotNegative(elderlyPeople, "elderlyPeople");
    requireNotNegative(disabledPeople, "disabledPeople");
    requireAtMost(children, "children", peopleAffected, "peopleAffected");
    requireAtMost(elderlyPeople, "elderlyPeople", peopleAffected, "peopleAffected");
    requireAtMost(disabledPeople, "disabledPeople", peopleAffected, "peopleAffected");
    if (population != null) {
      requireAtMost(peopleAffected, "peopleAffected", population, "population");
    }
  }

  public Optional<Integer> knownPopulation() {
    return Optional.ofNullable(population);
  }

  /** Children, elderly people and people with a disability. */
  public int vulnerablePopulation() {
    return children + elderlyPeople + disabledPeople;
  }

  private static void requireNotNegative(int value, String name) {
    if (value < 0) {
      throw new IllegalArgumentException(name + " must not be negative");
    }
  }

  private static void requireAtMost(int value, String name, int limit, String limitName) {
    if (value > limit) {
      throw new InconsistentPopulationImpactException(name, limitName);
    }
  }
}
