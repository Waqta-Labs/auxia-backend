package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RecordAidDeliveredCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The plan of a delivered zone is executed and the zone is served. An executed plan keeps
 * counting as aid the zone already has, so later recommendations give it only what it still
 * lacks.
 */
@Service
public class RecordAidDeliveredCommandHandler {

  private final DistributionPlanRepository distributionPlanRepository;
  private final ZoneRepository zoneRepository;

  public RecordAidDeliveredCommandHandler(
      DistributionPlanRepository distributionPlanRepository, ZoneRepository zoneRepository) {
    this.distributionPlanRepository = distributionPlanRepository;
    this.zoneRepository = zoneRepository;
  }

  /**
   * @throws DistributionPlanNotApprovedException if the plan is not approved
   */
  @Transactional
  public DistributionPlan handle(RecordAidDeliveredCommand command) {
    DistributionPlan plan =
        distributionPlanRepository
            .findById(command.distributionPlanId())
            .orElseThrow(() -> new DistributionPlanNotFoundException(command.distributionPlanId()));
    Zone zone =
        zoneRepository
            .findById(plan.zoneId())
            .orElseThrow(() -> new ZoneNotFoundException(plan.zoneId()));
    plan.markExecuted();
    zone.markServed(command.deliveredAt());
    zoneRepository.save(zone);
    return distributionPlanRepository.save(plan);
  }
}
