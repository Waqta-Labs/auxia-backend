package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What Emergency Management offers to the other bounded contexts, which run in the same process.
 * Traceability uses it to check a delivery against its approved plan and to name the zones of
 * its reports. It receives and returns
 * UUIDs and simple records, so no other context depends on the domain of Emergency Management.
 */
@Service
public class EmergencyManagementContextFacade {

  private final DistributionPlanRepository distributionPlanRepository;
  private final ZoneRepository zoneRepository;

  public EmergencyManagementContextFacade(
      DistributionPlanRepository distributionPlanRepository, ZoneRepository zoneRepository) {
    this.distributionPlanRepository = distributionPlanRepository;
    this.zoneRepository = zoneRepository;
  }

  /** Name of each zone that exists; an unknown one is left out. */
  @Transactional(readOnly = true)
  public Map<UUID, String> fetchZoneNames(Collection<UUID> zoneIds) {
    Map<UUID, String> names = new HashMap<>();
    for (UUID zoneId : new HashSet<>(zoneIds)) {
      zoneRepository.findById(new ZoneId(zoneId)).ifPresent(zone -> names.put(zoneId, zone.name()));
    }
    return names;
  }

  /** What an approved plan delivers; empty if the plan does not exist or is not approved. */
  @Transactional(readOnly = true)
  public List<PlannedQuantityResource> fetchApprovedPlanQuantities(UUID distributionPlanId) {
    return distributionPlanRepository
        .findById(new DistributionPlanId(distributionPlanId))
        .filter(DistributionPlan::isApproved)
        .map(
            plan ->
                plan.distributedQuantities().stream()
                    .map(
                        quantity ->
                            new PlannedQuantityResource(
                                quantity.resourceId().value(), quantity.quantity()))
                    .toList())
        .orElse(List.of());
  }
}
