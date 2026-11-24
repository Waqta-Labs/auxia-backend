package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import java.util.Optional;

/** Asks Resource Management about the brigade that took a distribution plan. */
@FunctionalInterface
public interface BrigadeTrackingPort {

  /**
   * Base of the brigade that is still delivering the plan.
   *
   * @return empty when no brigade took the plan or its delivery is already confirmed
   */
  Optional<Geolocation> baseOfBrigadeDelivering(DistributionPlanId planId);
}
