package com.waqtalabs.auxia.resourcemanagement.interfaces.rest;

import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionZones.MissionZone;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetMyMissionsQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.MissionQueryService;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.MissionQueryService.Mission;
import com.waqtalabs.auxia.resourcemanagement.application.queryservices.MissionQueryService.MyMissions;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.rest.resources.MyMissionsResource;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Missions of the signed-in brigadist, for the mobile app (A_07). */
@RestController
@RequestMapping("/api/v1/me/missions")
@Tag(name = "Brigades")
public class MissionController {

  private final MissionQueryService missions;

  public MissionController(MissionQueryService missions) {
    this.missions = missions;
  }

  @GetMapping
  @PreAuthorize("hasRole('FIELD_BRIGADE')")
  @Operation(
      summary = "Missions of my brigade",
      description =
          "The assignments of the brigade the account is a member of: the one in progress first"
              + " (ASSIGNED), then the completed ones, from the most recent. Empty when the"
              + " account is not a member of any brigade.")
  public MyMissionsResource myMissions(@AuthenticationPrincipal AuthenticatedUser user) {
    MyMissions mine = missions.handle(new GetMyMissionsQuery(new UserId(user.userId())));
    return new MyMissionsResource(
        mine.brigade().map(brigade -> brigade.id().value()).orElse(null),
        mine.brigade().map(Brigade::name).orElse(null),
        mine.missions().stream().map(MissionController::toResource).toList());
  }

  private static MyMissionsResource.Mission toResource(Mission mission) {
    MissionZone zone = mission.zone();
    return new MyMissionsResource.Mission(
        mission.deliveryId().orElse(null),
        mission.planId(),
        zone.zoneId(),
        zone.zoneName(),
        mission.status() == AssignmentStatus.IN_PROGRESS ? "ASSIGNED" : "COMPLETED",
        mission.category(),
        mission.assignedAt(),
        mission.completedAt().orElse(null),
        zone.score(),
        zone.priorityLevel(),
        zone.reasons().stream()
            .map(reason -> new MyMissionsResource.Reason(reason.feature(), reason.contribution()))
            .toList(),
        new MyMissionsResource.People(
            zone.peopleAffected(), zone.children(), zone.elderly(), zone.disabled()),
        zone.latitude() == null
            ? null
            : new MyMissionsResource.Location(zone.latitude(), zone.longitude()),
        zone.distanceKm(),
        zone.etaMinutes(),
        mission.items().stream()
            .map(
                item ->
                    new MyMissionsResource.Item(
                        item.resourceId().value(),
                        item.name(),
                        item.unit() == null ? null : item.unit().name(),
                        item.quantity()))
            .toList());
  }
}
