package com.waqtalabs.auxia.traceability.application.ports;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ResourceId;
import java.util.Map;

/** Reads from Emergency Management what an approved plan delivers. */
@FunctionalInterface
public interface DistributionPlanPort {

  /** Quantity of each resource that the plan holds; empty if the plan does not exist. */
  Map<ResourceId, Integer> plannedQuantities(DistributionPlanId planId);
}
