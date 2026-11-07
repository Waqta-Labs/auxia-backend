package com.waqtalabs.auxia.emergencymanagement.infrastructure.integration.resourcemanagement;

import com.waqtalabs.auxia.emergencymanagement.application.ports.BrigadeTrackingPort;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.ResourceManagementContextFacade;
import org.springframework.stereotype.Component;

/** Adapter to the brigade assignments of Resource Management, through its Open Host Service. */
@Component
class ResourceManagementBrigadeTrackingAdapter implements BrigadeTrackingPort {

  private final ResourceManagementContextFacade resourceManagement;

  ResourceManagementBrigadeTrackingAdapter(ResourceManagementContextFacade resourceManagement) {
    this.resourceManagement = resourceManagement;
  }

  @Override
  public boolean hasBrigade(DistributionPlanId planId) {
    return resourceManagement.hasAssignedBrigade(planId.value());
  }
}
