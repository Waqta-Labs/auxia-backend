package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository: the most recent plan first, then the last one saved. */
public class InMemoryDistributionPlanRepository implements DistributionPlanRepository {

  private final Map<DistributionPlanId, DistributionPlan> plans = new LinkedHashMap<>();

  @Override
  public DistributionPlan save(DistributionPlan plan) {
    plans.put(plan.id(), plan);
    return plan;
  }

  @Override
  public Optional<DistributionPlan> findById(DistributionPlanId id) {
    return Optional.ofNullable(plans.get(id));
  }

  @Override
  public List<DistributionPlan> findByZoneId(ZoneId zoneId, OrganizationId organizationId) {
    return mostRecentFirst(
        plans.values().stream()
            .filter(plan -> plan.zoneId().equals(zoneId) && plan.belongsTo(organizationId))
            .toList());
  }

  @Override
  public List<DistributionPlan> findByEmergencyId(
      EmergencyId emergencyId, OrganizationId organizationId) {
    return mostRecentFirst(
        plans.values().stream()
            .filter(
                plan -> plan.emergencyId().equals(emergencyId) && plan.belongsTo(organizationId))
            .toList());
  }

  @Override
  public boolean hasPendingPlans(EmergencyId emergencyId, OrganizationId organizationId) {
    return findByEmergencyId(emergencyId, organizationId).stream()
        .anyMatch(DistributionPlan::isPending);
  }

  private static List<DistributionPlan> mostRecentFirst(List<DistributionPlan> plans) {
    List<DistributionPlan> sorted = new ArrayList<>(plans.reversed());
    sorted.sort(Comparator.comparing(DistributionPlan::createdAt).reversed());
    return sorted;
  }
}
