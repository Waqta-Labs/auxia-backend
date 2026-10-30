package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ManualPriority;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ScoreContribution;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyScore;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.math.BigDecimal;
import java.util.Optional;

final class ZoneJpaMapper {

  private ZoneJpaMapper() {}

  static Zone toDomain(ZoneJpaEntity entity) {
    Geolocation geolocation =
        entity.latitude == null || entity.longitude == null
            ? null
            : new Geolocation(entity.latitude, entity.longitude);
    return Zone.reconstitute(
        new ZoneId(entity.id),
        new EmergencyId(entity.emergencyId),
        entity.name,
        geolocation,
        new PopulationImpact(
            entity.population,
            entity.familiesAffected,
            entity.peopleAffected,
            entity.children,
            entity.elderlyPeople,
            entity.disabledPeople),
        entity.damageLevel,
        entity.status,
        urgencyScore(entity),
        entity.lastAidReceivedAt,
        entity.manualPriorityBy == null
            ? null
            : new ManualPriority(
                new AuthorityId(entity.manualPriorityBy),
                entity.manualPriorityAt,
                entity.manualPriorityReason));
  }

  /** Copies the aggregate into a new or managed entity, keeping the columns it does not own. */
  static void copy(Zone zone, ZoneJpaEntity entity) {
    entity.id = zone.id().value();
    entity.emergencyId = zone.emergencyId().value();
    entity.name = zone.name();
    entity.latitude = zone.geolocation().map(Geolocation::latitude).orElse(null);
    entity.longitude = zone.geolocation().map(Geolocation::longitude).orElse(null);
    PopulationImpact impact = zone.populationImpact();
    entity.population = impact.population();
    entity.familiesAffected = impact.familiesAffected();
    entity.peopleAffected = impact.peopleAffected();
    entity.children = impact.children();
    entity.elderlyPeople = impact.elderlyPeople();
    entity.disabledPeople = impact.disabledPeople();
    entity.damageLevel = zone.damageLevel();
    entity.status = zone.status();
    entity.lastAidReceivedAt = zone.lastAidReceivedAt().orElse(null);
    entity.manualPriorityBy =
        zone.manualPriority().map(priority -> priority.setBy().value()).orElse(null);
    entity.manualPriorityAt = zone.manualPriority().map(ManualPriority::setAt).orElse(null);
    entity.manualPriorityReason = zone.manualPriority().map(ManualPriority::reason).orElse(null);
    Optional<UrgencyScore> score = zone.urgencyScore();
    entity.urgencyScore = score.map(s -> BigDecimal.valueOf(s.value())).orElse(null);
    entity.urgencyLevel = score.map(UrgencyScore::level).orElse(null);
    entity.urgencyTopFeatures =
        score
            .map(
                s ->
                    s.topFeatures().stream()
                        .map(f -> new ScoreContributionJson(f.feature(), f.contribution()))
                        .toList())
            .orElse(null);
    entity.urgencyModelVersion = score.map(UrgencyScore::modelVersion).orElse(null);
    entity.urgencyCalculatedAt = score.map(UrgencyScore::calculatedAt).orElse(null);
  }

  private static UrgencyScore urgencyScore(ZoneJpaEntity entity) {
    if (entity.urgencyScore == null) {
      return null;
    }
    return new UrgencyScore(
        entity.urgencyScore.doubleValue(),
        entity.urgencyLevel,
        entity.urgencyTopFeatures.stream()
            .map(f -> new ScoreContribution(f.feature(), f.contribution()))
            .toList(),
        entity.urgencyModelVersion,
        entity.urgencyCalculatedAt);
  }
}
