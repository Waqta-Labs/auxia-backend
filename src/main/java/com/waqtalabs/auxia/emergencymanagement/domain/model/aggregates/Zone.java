package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InsufficientScoringDataException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

/** Impact, urgency score and attention status of an area affected by an emergency. */
public final class Zone {

  private final ZoneId id;
  private final EmergencyId emergencyId;
  private final String name;
  private final Geolocation geolocation;
  private final PopulationImpact populationImpact;
  private final DamageLevel damageLevel;
  private ZoneStatus status;
  private UrgencyScore urgencyScore;

  private Zone(
      ZoneId id,
      EmergencyId emergencyId,
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel,
      ZoneStatus status,
      UrgencyScore urgencyScore) {
    this.id = Objects.requireNonNull(id, "id");
    this.emergencyId = Objects.requireNonNull(emergencyId, "emergencyId");
    this.name = requireText(name);
    this.geolocation = geolocation;
    this.populationImpact = Objects.requireNonNull(populationImpact, "populationImpact");
    this.damageLevel = Objects.requireNonNull(damageLevel, "damageLevel");
    this.status = Objects.requireNonNull(status, "status");
    this.urgencyScore = urgencyScore;
  }

  /**
   * Registers a zone in {@code REGISTERED} status, without an urgency score. The geolocation is
   * optional. Use {@link Emergency#registerZone}, which first checks that the emergency is still
   * active.
   */
  static Zone register(
      EmergencyId emergencyId,
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel) {
    return new Zone(
        ZoneId.generate(),
        emergencyId,
        name,
        geolocation,
        populationImpact,
        damageLevel,
        ZoneStatus.REGISTERED,
        null);
  }

  /**
   * Rebuilds a zone that is already stored, without registering it again.
   *
   * @param urgencyScore {@code null} while the score has not been calculated
   */
  public static Zone reconstitute(
      ZoneId id,
      EmergencyId emergencyId,
      String name,
      Geolocation geolocation,
      PopulationImpact populationImpact,
      DamageLevel damageLevel,
      ZoneStatus status,
      UrgencyScore urgencyScore) {
    return new Zone(
        id, emergencyId, name, geolocation, populationImpact, damageLevel, status, urgencyScore);
  }

  public boolean belongsTo(EmergencyId emergencyId) {
    return this.emergencyId.equals(emergencyId);
  }

  /**
   * Whether the urgency score can be calculated. The damage level and the people affected are
   * always known; the population of the zone is optional (US-04, scenario 2).
   */
  public boolean hasSufficientDataForScoring() {
    return populationImpact.knownPopulation().isPresent();
  }

  /**
   * Variables from which the urgency score is calculated. For each one it takes the most severe
   * value between the zone and its processed field reports: the highest damage level, the largest
   * counts and every need mentioned. The data registered for the zone does not change, and a new
   * report never lowers the urgency of the zone.
   *
   * @param reports structured data of the processed field reports of this zone
   * @throws InsufficientScoringDataException if the population of the zone is unknown
   */
  public UrgencyIndicators urgencyIndicators(List<StructuredFieldData> reports) {
    int population =
        populationImpact
            .knownPopulation()
            .orElseThrow(() -> new InsufficientScoringDataException(id));
    DamageLevel mostSevereDamage =
        reports.stream()
            .map(StructuredFieldData::damageLevel)
            .filter(Objects::nonNull)
            .reduce(damageLevel, Zone::mostSevere);
    Set<Need> needs = EnumSet.noneOf(Need.class);
    reports.forEach(report -> needs.addAll(report.needs()));
    return new UrgencyIndicators(
        mostSevereDamage,
        population,
        largest(populationImpact.familiesAffected(), reports, StructuredFieldData::familiesAffected),
        largest(populationImpact.peopleAffected(), reports, StructuredFieldData::peopleAffected),
        largest(populationImpact.children(), reports, StructuredFieldData::children),
        largest(populationImpact.elderlyPeople(), reports, StructuredFieldData::elderlyPeople),
        largest(populationImpact.disabledPeople(), reports, StructuredFieldData::disabledPeople),
        needs);
  }

  /** Applies a new urgency score and moves the zone to {@code PRIORITIZED}. */
  public void updateUrgencyScore(UrgencyScore urgencyScore) {
    this.urgencyScore = Objects.requireNonNull(urgencyScore, "urgencyScore");
    status = ZoneStatus.PRIORITIZED;
  }

  public ZoneId id() {
    return id;
  }

  public EmergencyId emergencyId() {
    return emergencyId;
  }

  public String name() {
    return name;
  }

  public Optional<Geolocation> geolocation() {
    return Optional.ofNullable(geolocation);
  }

  public PopulationImpact populationImpact() {
    return populationImpact;
  }

  public DamageLevel damageLevel() {
    return damageLevel;
  }

  public ZoneStatus status() {
    return status;
  }

  /** Empty until the score is calculated for the first time. */
  public Optional<UrgencyScore> urgencyScore() {
    return Optional.ofNullable(urgencyScore);
  }

  private static DamageLevel mostSevere(DamageLevel first, DamageLevel second) {
    return first.compareTo(second) >= 0 ? first : second;
  }

  private static int largest(
      int registered,
      List<StructuredFieldData> reports,
      Function<StructuredFieldData, Integer> count) {
    return reports.stream().map(count).filter(Objects::nonNull).reduce(registered, Math::max);
  }

  private static String requireText(String value) {
    Objects.requireNonNull(value, "name");
    if (value.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    return value.strip();
  }
}
