package com.waqtalabs.auxia.emergencymanagement.application.ports;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.Optional;

/** Asks Resource Management for a brigade to deliver an approved plan. */
public interface BrigadeAssignmentPort {

  /**
   * Assigns the most suitable available brigade of the organization to the plan.
   *
   * @param zoneLocation where the zone is; empty when it has no coordinates
   * @return the brigade, or empty when no brigade can take the plan now
   */
  Optional<BrigadeId> assign(
      OrganizationId organizationId,
      DistributionPlanId planId,
      ZoneId zoneId,
      Optional<Geolocation> zoneLocation);
}
