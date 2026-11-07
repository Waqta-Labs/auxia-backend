package com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Missions of the signed-in brigadist (A_07). {@code brigadeId} and {@code brigadeName} are
 * {@code null}, and the list empty, when the account is not a member of any brigade.
 */
public record MyMissionsResource(UUID brigadeId, String brigadeName, List<Mission> missions) {

  /** A mission card. Nullable values are {@code null} when unknown. */
  public record Mission(
      @Schema(description = "Delivery to register (A_08); null until it is opened")
          UUID deliveryId,
      UUID planId,
      UUID zoneId,
      String zoneName,
      @Schema(example = "ASSIGNED", allowableValues = {"ASSIGNED", "COMPLETED"}) String status,
      @Schema(example = "MEDICAL", allowableValues = {"MEDICAL", "SUPPLIES"}) String category,
      LocalDateTime assignedAt,
      LocalDateTime completedAt,
      @Schema(example = "94.0") Double score,
      @Schema(example = "CRITICAL") String priorityLevel,
      @Schema(description = "Why this priority, from the highest contribution")
          List<Reason> reasons,
      People people,
      Location location,
      @Schema(description = "From the base of the brigade, in a straight line", example = "3.2")
          Double distanceKm,
      @Schema(description = "Travel from the base at 30 km/h", example = "7") Integer etaMinutes,
      List<Item> items) {}

  public record Reason(String feature, double contribution) {}

  public record People(int peopleAffected, int children, int elderly, int disabled) {}

  public record Location(double latitude, double longitude) {}

  public record Item(UUID resourceId, String name, String unit, int quantity) {}
}
