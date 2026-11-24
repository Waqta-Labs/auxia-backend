package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import java.util.List;
import java.util.UUID;

/**
 * The zone a brigade goes to with a plan, as its mission card shows it (A_07). Nullable values
 * are {@code null} when the zone has no score or no coordinates.
 *
 * @param distanceKm from the base of the brigade, in a straight line
 * @param etaMinutes travel from the base at 30 km/h
 * @param items what the plan delivers
 */
public record MissionZoneResource(
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
    List<PlannedQuantityResource> items) {

  /** A variable that weighed in the score, from the highest contribution. */
  public record Reason(String feature, double contribution) {}
}
