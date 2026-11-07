package com.waqtalabs.auxia.resourcemanagement.infrastructure.integration.missions;

import com.waqtalabs.auxia.resourcemanagement.application.ports.MissionDeliveries;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.interfaces.acl.TraceabilityContextFacade;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Adapter to the deliveries of Traceability, through its Open Host Service. */
@Component
class TraceabilityMissionDeliveriesAdapter implements MissionDeliveries {

  private final TraceabilityContextFacade traceability;

  TraceabilityMissionDeliveriesAdapter(TraceabilityContextFacade traceability) {
    this.traceability = traceability;
  }

  @Override
  public Optional<UUID> deliveryOf(DistributionPlanId planId) {
    return traceability.fetchDeliveryOfPlan(planId.value());
  }
}
