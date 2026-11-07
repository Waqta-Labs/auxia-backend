package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;

/** Asks Resource Management whether a brigade took a distribution plan. */
@FunctionalInterface
public interface BrigadeTrackingPort {

  /** Whether a brigade was assigned to the plan, whether it is still delivering it or done. */
  boolean hasBrigade(DistributionPlanId planId);
}
