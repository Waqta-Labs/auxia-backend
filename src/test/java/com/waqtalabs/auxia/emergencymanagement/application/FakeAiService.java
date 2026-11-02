package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.application.ports.FieldReportStructuringService;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AvailableResource;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionRecommendation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PrioritizedZone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ResourceQuantity;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneAllocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.services.DistributionOptimizationService;
import com.waqtalabs.auxia.emergencymanagement.domain.services.UrgencyScoringService;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.services.StaffAssignmentService;
import com.waqtalabs.auxia.shared.application.exceptions.ExternalServiceUnavailableException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 *   <li>The distribution recommendation is what a test set with {@link #willRecommend}, and
 *       otherwise each resource goes, all of it, to the most urgent zone that still needs its
 *       category. A zone no longer needs a category it already has units of. A zone is FULL
 *       when it has every category it needs, NONE when it lacks some and receives nothing,
 *       and PARTIAL otherwise.
 *   <li>The suggested brigade is the closest candidate to the zone, the first one on a tie, as
 *       in rules-v1.
 *   <li>{@link #becomeUnavailable()} makes every call fail as if the service did not answer.
 * </ul>
 */
public class FakeAiService
    implements FieldReportStructuringService,
        UrgencyScoringService,
        DistributionOptimizationService,
        StaffAssignmentService {

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
  private final List<List<PrioritizedZone>> recommendationCalls = new CopyOnWriteArrayList<>();
  private volatile DistributionRecommendation recommendation;
  private volatile boolean unavailable;

  public void willStructure(String text, StructuredFieldData data) {
    structuredTexts.put(text, data);
  }

  /** The next recommendations are this one, until {@link #reset()}. */
  public void willRecommend(DistributionRecommendation recommendation) {
    this.recommendation = recommendation;
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
    recommendationCalls.clear();
    recommendation = null;
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

  /** Zones sent to each recommendation, in order. */
  public List<List<PrioritizedZone>> recommendationCalls() {
    return new ArrayList<>(recommendationCalls);
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

  @Override
  public BrigadeId suggestBrigade(List<Brigade> candidates, Location zoneLocation) {
    failIfUnavailable();
    return candidates.stream()
        .min(Comparator.comparingDouble(brigade -> brigade.baseLocation().distanceTo(zoneLocation)))
        .orElseThrow()
        .id();
  }

  @Override
  public DistributionRecommendation recommend(
      List<PrioritizedZone> zones, List<AvailableResource> inventory) {
    failIfUnavailable();
    recommendationCalls.add(List.copyOf(zones));
    if (recommendation != null) {
      return recommendation;
    }
    List<PrioritizedZone> byUrgency =
        zones.stream()
            .sorted(Comparator.comparingDouble(PrioritizedZone::score).reversed())
            .toList();
    Map<ZoneId, List<ResourceQuantity>> items = new LinkedHashMap<>();
    Map<ZoneId, Set<Need>> covered = new LinkedHashMap<>();
    zones.forEach(zone -> items.put(zone.zoneId(), new ArrayList<>()));
    for (AvailableResource resource : inventory) {
      Need category = Need.valueOf(resource.category());
      byUrgency.stream()
          .filter(zone -> stillNeeds(zone).contains(category))
          .findFirst()
          .ifPresent(
              zone -> {
                items
                    .get(zone.zoneId())
                    .add(new ResourceQuantity(resource.resourceId(), resource.available()));
                covered.computeIfAbsent(zone.zoneId(), id -> new HashSet<>()).add(category);
              });
    }
    List<ZoneAllocation> allocations =
        zones.stream()
            .map(
                zone -> {
                  Set<Need> received = covered.getOrDefault(zone.zoneId(), Set.of());
                  Set<Need> missing = new HashSet<>(stillNeeds(zone));
                  missing.removeAll(received);
                  Coverage coverage =
                      missing.isEmpty()
                          ? Coverage.FULL
                          : received.isEmpty() ? Coverage.NONE : Coverage.PARTIAL;
                  return new ZoneAllocation(zone.zoneId(), items.get(zone.zoneId()), coverage);
                })
            .toList();
    return new DistributionRecommendation(
        allocations, "Fake: each resource to the most urgent zone", MODEL_VERSION);
  }

  private static Set<Need> stillNeeds(PrioritizedZone zone) {
    Set<Need> needs = new HashSet<>(zone.needs());
    zone.alreadyAllocated().forEach(
        (category, quantity) -> {
          if (quantity > 0) {
            needs.remove(category);
          }
        });
    return needs;
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
