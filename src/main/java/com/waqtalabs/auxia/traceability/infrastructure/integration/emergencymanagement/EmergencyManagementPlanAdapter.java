package com.waqtalabs.auxia.traceability.infrastructure.integration.emergencymanagement;

import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.EmergencyManagementContextFacade;
import com.waqtalabs.auxia.emergencymanagement.interfaces.acl.PlannedQuantityResource;
import com.waqtalabs.auxia.traceability.application.ports.DistributionPlanPort;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Adapter to the plans of Emergency Management, through its Open Host Service. */
@Component
class EmergencyManagementPlanAdapter implements DistributionPlanPort {

  private final EmergencyManagementContextFacade emergencyManagement;

  EmergencyManagementPlanAdapter(EmergencyManagementContextFacade emergencyManagement) {
    this.emergencyManagement = emergencyManagement;
  }

  @Override
  public Map<ResourceId, Integer> plannedQuantities(DistributionPlanId planId) {
    return emergencyManagement.fetchApprovedPlanQuantities(planId.value()).stream()
        .collect(
            Collectors.toMap(
                planned -> new ResourceId(planned.resourceId()),
                PlannedQuantityResource::quantity));
  }
}
