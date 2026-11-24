package com.waqtalabs.auxia.resourcemanagement.application.ports;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import java.util.Optional;
import java.util.UUID;

/** Asks Traceability for the delivery that a brigade registers for a plan (A_08). */
@FunctionalInterface
public interface MissionDeliveries {

  /** The delivery of the plan; empty before Traceability opens it. */
  Optional<UUID> deliveryOf(DistributionPlanId planId);
}
