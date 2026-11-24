package com.waqtalabs.auxia.emergencymanagement.domain.services;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Finds the emergency, and if possible the zone, that the SOS of a citizen belongs to.
 *
 * <p>Zones are points, not areas, so the SOS goes to the nearest zone of an active emergency
 * when it is at most {@value #ZONE_RADIUS_KM} km away. Farther, up to {@value
 * #EMERGENCY_RADIUS_KM} km, it goes to the emergency of that zone and stays pending zone
 * assignment. Without a location, or far from every zone, it goes to the only active emergency
 * if there is just one. Otherwise it cannot be placed.
 */
public class SosPlacementService {

  public static final double ZONE_RADIUS_KM = 5;
  public static final double EMERGENCY_RADIUS_KM = 50;

  /**
   * Where an SOS belongs.
   *
   * @param zone empty when the SOS stays pending zone assignment
   */
  public record Placement(EmergencyId emergencyId, Optional<Zone> zone) {

    public Placement {
      Objects.requireNonNull(emergencyId, "emergencyId");
      Objects.requireNonNull(zone, "zone");
    }
  }

  /**
   * @param location where the SOS was sent from, empty without GPS
   * @param activeEmergencies the emergencies that are still active
   * @param zones the zones of those emergencies; zones of other emergencies are ignored
   * @return empty when the SOS cannot be placed in any emergency
   */
  public Optional<Placement> place(
      Optional<Geolocation> location, List<Emergency> activeEmergencies, List<Zone> zones) {
    Set<EmergencyId> active =
        activeEmergencies.stream()
            .filter(Emergency::isActive)
            .map(Emergency::id)
            .collect(Collectors.toSet());
    Optional<Placement> nearby =
        location.flatMap(
            point ->
                zones.stream()
                    .filter(zone -> active.contains(zone.emergencyId()))
                    .filter(zone -> zone.geolocation().isPresent())
                    .map(zone -> new Candidate(zone, point.kilometersTo(zone.geolocation().get())))
                    .filter(candidate -> candidate.kilometers() <= EMERGENCY_RADIUS_KM)
                    .min(Comparator.comparingDouble(Candidate::kilometers))
                    .map(Candidate::placement));
    if (nearby.isPresent()) {
      return nearby;
    }
    return active.size() == 1
        ? Optional.of(new Placement(active.iterator().next(), Optional.empty()))
        : Optional.empty();
  }

  private record Candidate(Zone zone, double kilometers) {

    Placement placement() {
      return new Placement(
          zone.emergencyId(),
          kilometers <= ZONE_RADIUS_KM ? Optional.of(zone) : Optional.empty());
    }
  }
}
