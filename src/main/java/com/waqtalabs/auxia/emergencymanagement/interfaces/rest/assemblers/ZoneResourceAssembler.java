package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.application.commands.UpdateZonePopulationImpactCommand;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneHistoryEntry;
import com.waqtalabs.auxia.emergencymanagement.application.queryservices.ZoneRankingEntry;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.GeolocationResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ManualPriorityResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.PopulationImpactResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterZoneRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ScoreContributionResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneDecisionResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneRankingItemResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneScoreResource;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class ZoneResourceAssembler {

  private ZoneResourceAssembler() {}

  /** Missing counts are 0, as in the database. */
  public static RegisterZoneCommand toCommand(
      UUID emergencyId, RegisterZoneRequest request, AuthorityId registeredBy) {
    GeolocationResource geolocation = request.geolocation();
    PopulationImpactResource impact = request.populationImpact();
    return new RegisterZoneCommand(
        new EmergencyId(emergencyId),
        request.name(),
        geolocation == null
            ? null
            : new Geolocation(geolocation.latitude(), geolocation.longitude()),
        new PopulationImpact(
            impact.population(),
            orZero(impact.familiesAffected()),
            orZero(impact.peopleAffected()),
            orZero(impact.children()),
            orZero(impact.elderlyPeople()),
            orZero(impact.disabledPeople())),
        request.damageLevel(),
        registeredBy);
  }

  /** Missing counts keep their current value. */
  public static UpdateZonePopulationImpactCommand toCommand(
      ZoneId zoneId, PopulationImpactResource request) {
    return new UpdateZonePopulationImpactCommand(
        zoneId,
        request.population(),
        request.familiesAffected(),
        request.peopleAffected(),
        request.children(),
        request.elderlyPeople(),
        request.disabledPeople());
  }

  public static ZoneResource toResource(Zone zone) {
    PopulationImpact impact = zone.populationImpact();
    Optional<UrgencyScore> score = zone.urgencyScore();
    return new ZoneResource(
        zone.id().value(),
        zone.emergencyId().value(),
        zone.name(),
        zone.geolocation()
            .map(location -> new GeolocationResource(location.latitude(), location.longitude()))
            .orElse(null),
        new PopulationImpactResource(
            impact.population(),
            impact.familiesAffected(),
            impact.peopleAffected(),
            impact.children(),
            impact.elderlyPeople(),
            impact.disabledPeople()),
        zone.damageLevel(),
        zone.status(),
        score.map(UrgencyScore::value).orElse(null),
        score.map(UrgencyScore::level).orElse(null),
        zone.lastAidReceivedAt().orElse(null),
        manualPriority(zone),
        zone.responsibleAuthority().map(AuthorityId::value).orElse(null));
  }

  public static ZoneDecisionResource toResource(ZoneHistoryEntry entry) {
    ZoneDecision decision = entry.decision();
    return new ZoneDecisionResource(
        decision.type(),
        decision.decidedAt(),
        decision.decidedBy().value(),
        entry.decidedByName(),
        decision.detail().orElse(null),
        decision.subjectId().orElse(null),
        decision.handedOverTo().map(AuthorityId::value).orElse(null),
        entry.handedOverToName());
  }

  public static ZoneRankingItemResource toRankingItem(ZoneRankingEntry entry) {
    Zone zone = entry.zone();
    Optional<UrgencyScore> score = zone.urgencyScore();
    return new ZoneRankingItemResource(
        zone.id().value(),
        zone.name(),
        zone.status(),
        zone.damageLevel(),
        zone.populationImpact().peopleAffected(),
        score.map(UrgencyScore::value).orElse(null),
        score.map(UrgencyScore::level).orElse(null),
        entry.needs(),
        manualPriority(zone));
  }

  private static ManualPriorityResource manualPriority(Zone zone) {
    return zone.manualPriority()
        .map(
            priority ->
                new ManualPriorityResource(
                    priority.reason(), priority.setBy().value(), priority.setAt()))
        .orElse(null);
  }

  /**
   * The score with its explanation. Without a score, it lists what the zone lacks: its population,
   * or nothing when the score is just pending because the AI Service did not answer.
   */
  public static ZoneScoreResource toScoreResource(Zone zone) {
    return zone.urgencyScore()
        .map(
            score ->
                new ZoneScoreResource(
                    zone.id().value(),
                    true,
                    score.value(),
                    score.level(),
                    score.topFeatures().stream()
                        .map(f -> new ScoreContributionResource(f.feature(), f.contribution()))
                        .toList(),
                    score.modelVersion(),
                    score.calculatedAt(),
                    List.of()))
        .orElseGet(
            () ->
                new ZoneScoreResource(
                    zone.id().value(),
                    false,
                    null,
                    null,
                    List.of(),
                    null,
                    null,
                    zone.hasSufficientDataForScoring() ? List.of() : List.of("population")));
  }

  private static int orZero(Integer count) {
    return count == null ? 0 : count;
  }
}
