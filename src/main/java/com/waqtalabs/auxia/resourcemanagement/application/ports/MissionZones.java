package com.waqtalabs.auxia.resourcemanagement.application.ports;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Asks Emergency Management for the zone a plan goes to, as a mission card shows it (A_07). */
@FunctionalInterface
public interface MissionZones {

  /**
   * @param base where the brigade leaves from, for the distance and the travel time
   * @return empty if the plan does not exist or was never approved
   */
  Optional<MissionZone> zoneOf(DistributionPlanId planId, Location base);

  /**
   * The zone of a mission. Nullable values are {@code null} when the zone has no score or no
   * coordinates.
   *
   * @param distanceKm from the base of the brigade, in a straight line
   * @param etaMinutes travel from the base at 30 km/h
   * @param items what the plan delivers
   */
  record MissionZone(
      UUID zoneId,
      String zoneName,
      Double score,
      String priorityLevel,
      List<Reason> reasons,
      int peopleAffected,
      int children,
      int elderly,
      int disabled,
      Double latitude,
      Double longitude,
      Double distanceKm,
      Integer etaMinutes,
      List<Item> items) {}

  /** A variable that weighed in the score of the zone. */
  record Reason(String feature, double contribution) {}

  /** A resource of the plan and how much of it. */
  record Item(ResourceId resourceId, int quantity) {}
}
