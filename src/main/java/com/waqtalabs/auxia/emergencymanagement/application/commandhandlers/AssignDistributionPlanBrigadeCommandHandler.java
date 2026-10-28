package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignDistributionPlanBrigadeCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.BrigadeAssignmentPort;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotApprovedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.NoAvailableBrigadeException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gets a brigade for an approved plan. It runs right after the approval and again when an
 * authority asks for it, because at approval time every brigade may be busy.
 */
@Service
public class AssignDistributionPlanBrigadeCommandHandler {

  private final DistributionPlanRepository distributionPlanRepository;
  private final ZoneRepository zoneRepository;
  private final BrigadeAssignmentPort brigadeAssignment;
  private final AuthorizationPort authorization;

  public AssignDistributionPlanBrigadeCommandHandler(
      DistributionPlanRepository distributionPlanRepository,
      ZoneRepository zoneRepository,
      BrigadeAssignmentPort brigadeAssignment,
      AuthorizationPort authorization) {
    this.distributionPlanRepository = distributionPlanRepository;
    this.zoneRepository = zoneRepository;
    this.brigadeAssignment = brigadeAssignment;
    this.authorization = authorization;
  }

  /**
   * @throws DistributionPlanNotApprovedException if the plan is not approved
   * @throws NoAvailableBrigadeException if no brigade of the organization can take it now
   */
  @Transactional
  public BrigadeId handle(AssignDistributionPlanBrigadeCommand command) {
    command
        .requestedBy()
        .ifPresent(authority -> DistributionPlans.requireActiveAuthority(authorization, authority));
    DistributionPlan plan =
        DistributionPlans.load(
            distributionPlanRepository, command.planId(), command.organizationId());
    if (!plan.isApproved()) {
      throw new DistributionPlanNotApprovedException(plan.id(), plan.status());
    }
    Zone zone =
        zoneRepository
            .findById(plan.zoneId())
            .orElseThrow(() -> new ZoneNotFoundException(plan.zoneId()));
    return brigadeAssignment
        .assign(plan.organizationId(), plan.id(), zone.id(), zone.geolocation())
        .orElseThrow(() -> new NoAvailableBrigadeException(plan.id()));
  }
}
