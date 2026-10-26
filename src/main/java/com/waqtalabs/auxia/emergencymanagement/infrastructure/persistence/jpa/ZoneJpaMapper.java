package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

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
        entity.status);
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
  }
}
