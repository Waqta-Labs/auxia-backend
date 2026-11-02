package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlanByIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlansByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetDistributionPlansByZoneIdQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Plans of the organization of the user. A plan of another organization is not found. */
@Service
@Transactional(readOnly = true)
public class DistributionPlanQueryService {

  private final DistributionPlanRepository distributionPlanRepository;
  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;

  public DistributionPlanQueryService(
      DistributionPlanRepository distributionPlanRepository,
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository) {
    this.distributionPlanRepository = distributionPlanRepository;
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
  }

  public DistributionPlan handle(GetDistributionPlanByIdQuery query) {
    return distributionPlanRepository
        .findById(query.planId())
        .filter(plan -> plan.belongsTo(query.organizationId()))
        .orElseThrow(() -> new DistributionPlanNotFoundException(query.planId()));
  }

  /** History of the zone, from the most recent plan (US-14, scenario 2). */
  public List<DistributionPlan> handle(GetDistributionPlansByZoneIdQuery query) {
    if (zoneRepository.findById(query.zoneId()).isEmpty()) {
      throw new ZoneNotFoundException(query.zoneId());
    }
    return distributionPlanRepository.findByZoneId(query.zoneId(), query.organizationId());
  }

  public List<DistributionPlan> handle(GetDistributionPlansByEmergencyIdQuery query) {
    if (emergencyRepository.findById(query.emergencyId()).isEmpty()) {
      throw new EmergencyNotFoundException(query.emergencyId());
    }
    return distributionPlanRepository
        .findByEmergencyId(query.emergencyId(), query.organizationId())
        .stream()
        .filter(plan -> query.status() == null || plan.status() == query.status())
        .toList();
  }
}
