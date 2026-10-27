package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.util.Objects;

/**
 * Points that one variable added to an urgency score, so the authority can explain the priority
 * of a zone (US-05).
 *
 * @param feature name of the variable, such as {@code damageLevel} or {@code vulnerablePopulation}
 * @param contribution score points, from 0 to 100
 */
public record ScoreContribution(String feature, double contribution) {

  public ScoreContribution {
    Objects.requireNonNull(feature, "feature");
    if (feature.isBlank()) {
      throw new IllegalArgumentException("feature must not be blank");
    }
    if (!(contribution >= 0 && contribution <= 100)) {
      throw new IllegalArgumentException("contribution must be between 0 and 100");
    }
  }
}
