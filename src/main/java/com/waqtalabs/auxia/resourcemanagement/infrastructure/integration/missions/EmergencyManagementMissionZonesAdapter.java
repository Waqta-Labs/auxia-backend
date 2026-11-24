package com.waqtalabs.auxia.resourcemanagement.infrastructure.integration.missions;

import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.EmergencyManagementContextFacade;
import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.MissionZoneResource;
import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionZones;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.Location;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adapter to the zones of Emergency Management, through its Open Host Service. */
@Component
class EmergencyManagementMissionZonesAdapter implements MissionZones {

  private final EmergencyManagementContextFacade emergencyManagement;

  EmergencyManagementMissionZonesAdapter(EmergencyManagementContextFacade emergencyManagement) {
    this.emergencyManagement = emergencyManagement;
  }

  @Override
  public Optional<MissionZone> zoneOf(DistributionPlanId planId, Location base) {
    return emergencyManagement
        .fetchMissionZone(planId.value(), base.latitude(), base.longitude())
        .map(EmergencyManagementMissionZonesAdapter::toMissionZone);
  }

  private static MissionZone toMissionZone(MissionZoneResource zone) {
    return new MissionZone(
        zone.zoneId(),
        zone.zoneName(),
        zone.score(),
        zone.priorityLevel(),
        zone.reasons().stream()
            .map(reason -> new Reason(reason.feature(), reason.contribution()))
            .toList(),
        zone.peopleAffected(),
        zone.children(),
        zone.elderly(),
        zone.disabled(),
        zone.latitude(),
        zone.longitude(),
        zone.distanceKm(),
        zone.etaMinutes(),
        zone.items().stream()
            .map(item -> new Item(new ResourceId(item.resourceId()), item.quantity()))
            .toList());
  }
}
