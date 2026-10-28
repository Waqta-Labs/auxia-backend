package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.resourcemanagement;

import com.waqtalabs.auxia.emergencymanagement.application.ports.BrigadeAssignmentPort;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ResourceManagementContextFacade;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ZoneLocationResource;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Adapter to the brigades of Resource Management, through its Open Host Service. */
@Component
class ResourceManagementBrigadeAdapter implements BrigadeAssignmentPort {

  private final ResourceManagementContextFacade resourceManagement;

  ResourceManagementBrigadeAdapter(ResourceManagementContextFacade resourceManagement) {
    this.resourceManagement = resourceManagement;
  }

  @Override
  public Optional<BrigadeId> assign(
      OrganizationId organizationId,
      DistributionPlanId planId,
      ZoneId zoneId,
      Optional<Geolocation> zoneLocation) {
    return resourceManagement
        .assignBrigade(
            organizationId.value(),
            planId.value(),
            zoneId.value(),
            zoneLocation
                .map(
                    location -> new ZoneLocationResource(location.latitude(), location.longitude()))
                .orElse(null))
        .map(BrigadeId::new);
  }
}
