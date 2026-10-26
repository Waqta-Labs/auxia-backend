package com.waqtalabs.auxia.emergencymanagement.interfaces.rest.assemblers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PopulationImpact;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.GeolocationResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.PopulationImpactResource;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.RegisterZoneRequest;
import com.waqtalabs.auxia.emergencymanagement.interfaces.rest.resources.ZoneResource;
import java.util.UUID;

public final class ZoneResourceAssembler {

  private ZoneResourceAssembler() {}

  /** Missing counts are 0, as in the database. */
  public static RegisterZoneCommand toCommand(UUID emergencyId, RegisterZoneRequest request) {
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
        request.damageLevel());
  }

  public static ZoneResource toResource(Zone zone) {
    PopulationImpact impact = zone.populationImpact();
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
        zone.status());
  }

  private static int orZero(Integer count) {
    return count == null ? 0 : count;
  }
}
