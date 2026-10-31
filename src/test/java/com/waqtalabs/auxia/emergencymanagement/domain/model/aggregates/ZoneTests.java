package com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyClosedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.InsufficientScoringDataException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ManualPriorityReasonRequiredException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AdministrativeLocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ManualPriority;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Need;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ZoneTests {

  private static final Geolocation ALTO_PERU = new Geolocation(-11.9436, -76.7078);
  private static final PopulationImpact IMPACT = new PopulationImpact(600, 35, 140, 40, 15, 0);

  private final Emergency emergency =
      Emergency.register(
          "Huaico Chosica 2026",
          EmergencyType.LANDSLIDE,
          null,
          new AdministrativeLocation("Lima", "Lima", "Lurigancho-Chosica"),
          LocalDate.of(2026, 3, 14));

  @Test
  void activeEmergencyRegistersZonesInRegisteredStatus() {
    Zone zone =
        emergency.registerZone(" Sector Alto Perú ", ALTO_PERU, IMPACT, DamageLevel.SEVERE);

    assertThat(zone.id()).isNotNull();
    assertThat(zone.emergencyId()).isEqualTo(emergency.id());
    assertThat(zone.belongsTo(emergency.id())).isTrue();
    assertThat(zone.name()).isEqualTo("Sector Alto Perú");
    assertThat(zone.geolocation()).contains(ALTO_PERU);
    assertThat(zone.populationImpact()).isEqualTo(IMPACT);
    assertThat(zone.damageLevel()).isEqualTo(DamageLevel.SEVERE);
    assertThat(zone.status()).isEqualTo(ZoneStatus.REGISTERED);
  }

  @Test
  void geolocationIsOptional() {
    Zone zone = emergency.registerZone("Quebrada Pedregal", null, IMPACT, DamageLevel.LOW);

    assertThat(zone.geolocation()).isEmpty();
  }

  @Test
  void closedEmergencyRejectsNewZones() {
    emergency.close();

    assertThatThrownBy(
            () -> emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE))
        .isInstanceOf(EmergencyClosedException.class);
  }

  @Test
  void blankNameIsRejected() {
    assertThatThrownBy(() -> emergency.registerZone(" ", null, IMPACT, DamageLevel.SEVERE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void zoneDoesNotBelongToAnotherEmergency() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);

    assertThat(zone.belongsTo(EmergencyId.generate())).isFalse();
  }

  @Test
  void registeredZoneHasNoUrgencyScore() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);

    assertThat(zone.urgencyScore()).isEmpty();
  }

  @Test
  void zoneWithoutPopulationHasNoScoreAvailable() {
    Zone known = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    Zone unknown =
        emergency.registerZone(
            "Quebrada Pedregal", null, new PopulationImpact(null, 0, 0, 0, 0, 0), DamageLevel.LOW);

    assertThat(known.hasSufficientDataForScoring()).isTrue();
    assertThat(unknown.hasSufficientDataForScoring()).isFalse();
    assertThatThrownBy(() -> unknown.urgencyIndicators(List.of()))
        .isInstanceOf(InsufficientScoringDataException.class);
  }

  @Test
  void withoutReportsTheIndicatorsAreTheDataOfTheZone() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);

    assertThat(zone.urgencyIndicators(List.of()))
        .isEqualTo(new UrgencyIndicators(DamageLevel.SEVERE, 600, 35, 140, 40, 15, 0, Set.of()));
  }

  @Test
  void indicatorsTakeTheMostSevereValueBetweenTheZoneAndItsReports() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    var critical =
        new StructuredFieldData(
            DamageLevel.CRITICAL, 20, 200, null, 30, 4, Set.of(Need.WATER), List.of());
    var lowerCounts =
        new StructuredFieldData(
            DamageLevel.MODERATE, 50, 90, 10, null, null, Set.of(Need.FOOD), List.of());

    UrgencyIndicators indicators = zone.urgencyIndicators(List.of(critical, lowerCounts));

    assertThat(indicators)
        .isEqualTo(
            new UrgencyIndicators(
                DamageLevel.CRITICAL, 600, 50, 200, 40, 30, 4, Set.of(Need.WATER, Need.FOOD)));
  }

  @Test
  void aReportWithLowerValuesDoesNotLowerTheIndicators() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    var mild =
        new StructuredFieldData(DamageLevel.LOW, 1, 2, 0, 0, 0, Set.of(), List.of());

    assertThat(zone.urgencyIndicators(List.of(mild)))
        .isEqualTo(zone.urgencyIndicators(List.of()));
  }

  @Test
  void zoneGetsItsPopulationLaterAndThenHasDataForScoring() {
    Zone zone =
        emergency.registerZone(
            "Quebrada Pedregal", null, new PopulationImpact(null, 0, 20, 0, 0, 0), DamageLevel.LOW);
    var known = new PopulationImpact(300, 5, 20, 0, 0, 0);

    zone.updatePopulationImpact(known);

    assertThat(zone.populationImpact()).isEqualTo(known);
    assertThat(zone.hasSufficientDataForScoring()).isTrue();
  }

  @Test
  void updatingTheScorePrioritizesTheZone() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    var score =
        new UrgencyScore(
            54.9,
            PriorityLevel.HIGH,
            List.of(new ScoreContribution("damageLevel", 30.0)),
            "rules-v1",
            LocalDateTime.of(2026, 3, 14, 9, 30));

    zone.updateUrgencyScore(score);

    assertThat(zone.urgencyScore()).contains(score);
    assertThat(zone.status()).isEqualTo(ZoneStatus.PRIORITIZED);
  }

  @Test
  void deliveredAidServesTheZoneAndKeepsTheLatestDate() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    var morning = LocalDateTime.of(2026, 3, 15, 10, 30);

    zone.markServed(morning);
    zone.markServed(morning.minusDays(1));

    assertThat(zone.status()).isEqualTo(ZoneStatus.SERVED);
    assertThat(zone.lastAidReceivedAt()).contains(morning);
    zone.updateUrgencyScore(
        new UrgencyScore(80.0, PriorityLevel.CRITICAL, List.of(), "rules-v1", morning));
    assertThat(zone.status()).isEqualTo(ZoneStatus.PRIORITIZED);
    assertThat(zone.lastAidReceivedAt()).contains(morning);
  }

  @Test
  void zoneIsPrioritizedByHandWithItsJustificationUntilTheMarkIsRemoved() {
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    var authority = new AuthorityId(UUID.randomUUID());
    var at = LocalDateTime.of(2026, 3, 15, 8, 0);

    zone.prioritizeManually(authority, " Riesgo inminente de un nuevo huaico ", at);

    assertThat(zone.isPrioritizedManually()).isTrue();
    assertThat(zone.manualPriority())
        .contains(new ManualPriority(authority, at, "Riesgo inminente de un nuevo huaico"));
    zone.removeManualPriority();
    assertThat(zone.manualPriority()).isEmpty();
    assertThatThrownBy(() -> zone.prioritizeManually(authority, "", at))
        .isInstanceOf(ManualPriorityReasonRequiredException.class);
  }

  @Test
  void responsibleAuthorityChangesHandsAndIsKept() {
    var registeredBy = new AuthorityId(UUID.randomUUID());
    var nextShift = new AuthorityId(UUID.randomUUID());
    Zone zone = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);
    assertThat(zone.responsibleAuthority()).isEmpty();

    zone.assignResponsible(registeredBy);
    zone.assignResponsible(nextShift);

    assertThat(zone.responsibleAuthority()).contains(nextShift);
    Zone stored =
        Zone.reconstitute(
            zone.id(),
            zone.emergencyId(),
            zone.name(),
            null,
            zone.populationImpact(),
            zone.damageLevel(),
            zone.status(),
            null,
            null,
            null,
            nextShift);
    assertThat(stored.responsibleAuthority()).contains(nextShift);
  }

  @Test
  void reconstitutedZoneKeepsItsStatus() {
    Zone stored = emergency.registerZone("Sector Alto Perú", null, IMPACT, DamageLevel.SEVERE);

    Zone zone =
        Zone.reconstitute(
            stored.id(),
            stored.emergencyId(),
            stored.name(),
            ALTO_PERU,
            stored.populationImpact(),
            stored.damageLevel(),
            ZoneStatus.PRIORITIZED,
            null,
            null,
            null,
            null);

    assertThat(zone.id()).isEqualTo(stored.id());
    assertThat(zone.status()).isEqualTo(ZoneStatus.PRIORITIZED);
    assertThat(zone.geolocation()).contains(ALTO_PERU);
  }
}
