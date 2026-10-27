package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * Urgency score of a zone with its explanation (US-04, US-05).
 *
 * <p>Every score keeps the variables that weighed the most and the version of the model that
 * calculated it, so a priority can always be justified.
 *
 * @param value from 0 to 100
 * @param topFeatures variables that weighed the most, from the highest contribution
 */
public record UrgencyScore(
    double value,
    PriorityLevel level,
    List<ScoreContribution> topFeatures,
    String modelVersion,
    LocalDateTime calculatedAt) {

  public UrgencyScore {
    if (!(value >= 0 && value <= 100)) {
      throw new IllegalArgumentException("value must be between 0 and 100");
    }
    Objects.requireNonNull(level, "level");
    topFeatures = List.copyOf(Objects.requireNonNull(topFeatures, "topFeatures"));
    Objects.requireNonNull(modelVersion, "modelVersion");
    if (modelVersion.isBlank()) {
      throw new IllegalArgumentException("modelVersion must not be blank");
    }
    Objects.requireNonNull(calculatedAt, "calculatedAt");
  }

  public boolean isCritical() {
    return level == PriorityLevel.CRITICAL;
  }
}
