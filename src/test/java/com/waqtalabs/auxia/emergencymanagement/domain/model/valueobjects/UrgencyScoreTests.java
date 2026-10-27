package com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class UrgencyScoreTests {

  private static final LocalDateTime CALCULATED_AT = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final List<ScoreContribution> TOP_FEATURES =
      List.of(new ScoreContribution("damageLevel", 40.0));

  @Test
  void onlyACriticalScoreIsCritical() {
    assertThat(score(84.1, PriorityLevel.CRITICAL).isCritical()).isTrue();
    assertThat(score(54.9, PriorityLevel.HIGH).isCritical()).isFalse();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 100.1, Double.NaN})
  void valueOutsideZeroAndOneHundredIsRejected(double value) {
    assertThatThrownBy(() -> score(value, PriorityLevel.LOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void blankModelVersionIsRejected() {
    assertThatThrownBy(
            () -> new UrgencyScore(50, PriorityLevel.HIGH, TOP_FEATURES, " ", CALCULATED_AT))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void contributionOutsideZeroAndOneHundredIsRejected() {
    assertThatThrownBy(() -> new ScoreContribution("needs", 101))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static UrgencyScore score(double value, PriorityLevel level) {
    return new UrgencyScore(value, level, TOP_FEATURES, "rules-v1", CALCULATED_AT);
  }
}
