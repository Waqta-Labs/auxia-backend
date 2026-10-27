package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.application.ports.FieldReportStructuringService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.services.UrgencyScoringService;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Test double of the AI Service, with predictable results.
 *
 * <ul>
 *   <li>Structuring returns what a test set with {@link #willStructure} for that text, and
 *       otherwise a report where nothing was identified.
 *   <li>The score is {@code 40 × damage + 60 × peopleAffected / population}, with the damage
 *       factors of rules-v1 (LOW 0.25 … CRITICAL 1) and the same priority thresholds (75, 50,
 *       25). Its top features are those two contributions.
 *   <li>{@link #becomeUnavailable()} makes every call fail as if the service did not answer.
 * </ul>
 */
public class FakeAiService implements FieldReportStructuringService, UrgencyScoringService {

  public static final String MODEL_VERSION = "fake-v1";
  public static final LocalDateTime CALCULATED_AT = LocalDateTime.of(2026, 3, 14, 9, 30);

  private static final StructuredFieldData NOTHING_IDENTIFIED =
      new StructuredFieldData(
          null,
          null,
          null,
          null,
          null,
          null,
          Set.of(),
          List.of(
              "damageLevel",
              "familiesAffected",
              "peopleAffected",
              "children",
              "elderlyPeople",
              "disabledPeople",
              "needs"));

  private final Map<String, StructuredFieldData> structuredTexts = new ConcurrentHashMap<>();
  private final List<String> structuringCalls = new CopyOnWriteArrayList<>();
  private final List<UrgencyIndicators> scoringCalls = new CopyOnWriteArrayList<>();
  private volatile boolean unavailable;

  public void willStructure(String text, StructuredFieldData data) {
    structuredTexts.put(text, data);
  }

  public void becomeUnavailable() {
    unavailable = true;
  }

  public void becomeAvailable() {
    unavailable = false;
  }

  /** Forgets every configured text and call, for tests that share the application context. */
  public void reset() {
    structuredTexts.clear();
    structuringCalls.clear();
    scoringCalls.clear();
    unavailable = false;
  }

  /** Texts sent to structure, in order. */
  public List<String> structuringCalls() {
    return new ArrayList<>(structuringCalls);
  }

  /** Indicators sent to score, in order. */
  public List<UrgencyIndicators> scoringCalls() {
    return new ArrayList<>(scoringCalls);
  }

  @Override
  public StructuredFieldData structure(String rawText) {
    failIfUnavailable();
    structuringCalls.add(rawText);
    return structuredTexts.getOrDefault(rawText, NOTHING_IDENTIFIED);
  }

  @Override
  public UrgencyScore calculateScore(UrgencyIndicators indicators) {
    failIfUnavailable();
    scoringCalls.add(indicators);
    double damage = round(40 * damageFactor(indicators.damageLevel()));
    double people =
        round(60 * Math.min(1.0, (double) indicators.peopleAffected() / indicators.population()));
    double score = round(damage + people);
    List<ScoreContribution> topFeatures = new ArrayList<>();
    topFeatures.add(new ScoreContribution("damageLevel", damage));
    if (people > 0) {
      topFeatures.add(new ScoreContribution("peopleAffected", people));
    }
    topFeatures.sort(Comparator.comparingDouble(ScoreContribution::contribution).reversed());
    return new UrgencyScore(score, level(score), topFeatures, MODEL_VERSION, CALCULATED_AT);
  }

  private void failIfUnavailable() {
    if (unavailable) {
      throw new ExternalServiceUnavailableException("AI Service", null);
    }
  }

  private static double damageFactor(DamageLevel level) {
    return switch (level) {
      case LOW -> 0.25;
      case MODERATE -> 0.5;
      case SEVERE -> 0.75;
      case CRITICAL -> 1.0;
    };
  }

  private static PriorityLevel level(double score) {
    if (score >= 75) {
      return PriorityLevel.CRITICAL;
    }
    if (score >= 50) {
      return PriorityLevel.HIGH;
    }
    return score >= 25 ? PriorityLevel.MEDIUM : PriorityLevel.LOW;
  }

  private static double round(double value) {
    return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
  }
}
