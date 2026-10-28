package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.List;
import java.util.Optional;

/** Every list holds the plans of one organization, from the most recent to the oldest. */
public interface DistributionPlanRepository {

  DistributionPlan save(DistributionPlan plan);

  Optional<DistributionPlan> findById(DistributionPlanId id);

  /** Plans of the zone, with their decisions and justifications: its history (US-14). */
  List<DistributionPlan> findByZoneId(ZoneId zoneId, OrganizationId organizationId);

  List<DistributionPlan> findByEmergencyId(EmergencyId emergencyId, OrganizationId organizationId);

  /** Whether the organization has proposed plans of the emergency still to decide. */
  boolean hasPendingPlans(EmergencyId emergencyId, OrganizationId organizationId);
}
