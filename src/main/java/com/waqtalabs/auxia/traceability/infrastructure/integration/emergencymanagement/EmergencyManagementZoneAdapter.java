package com.waqtalabs.auxia.traceability.infrastructure.integration.emergencymanagement;

import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.EmergencyManagementContextFacade;
import com.waqtalabs.auxia.traceability.application.ports.ZoneNamePort;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adapter to the zones of Emergency Management, through its Open Host Service. */
@Component
class EmergencyManagementZoneAdapter implements ZoneNamePort {

  private final EmergencyManagementContextFacade emergencyManagement;

  EmergencyManagementZoneAdapter(EmergencyManagementContextFacade emergencyManagement) {
    this.emergencyManagement = emergencyManagement;
  }

  @Override
  public Map<ZoneId, String> zoneNames(Collection<ZoneId> zoneIds) {
    return emergencyManagement
        .fetchZoneNames(zoneIds.stream().map(ZoneId::value).toList())
        .entrySet()
        .stream()
        .collect(Collectors.toMap(entry -> new ZoneId(entry.getKey()), Map.Entry::getValue));
  }
}
