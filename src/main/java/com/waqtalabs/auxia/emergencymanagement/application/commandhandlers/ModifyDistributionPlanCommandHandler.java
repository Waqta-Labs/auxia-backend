package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.ModifyDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.ResourceReservationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanModified;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes quantities of a proposed plan and its reservation (US-13). The new quantities cannot
 * exceed the available stock: the reservation fails, and nothing changes, if they do.
 */
@Service
public class ModifyDistributionPlanCommandHandler {

  private final DistributionPlanRepository distributionPlanRepository;
  private final ResourceReservationPort resourceReservation;
  private final AuthorizationPort authorization;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public ModifyDistributionPlanCommandHandler(
      DistributionPlanRepository distributionPlanRepository,
      ResourceReservationPort resourceReservation,
      AuthorizationPort authorization,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.distributionPlanRepository = distributionPlanRepository;
    this.resourceReservation = resourceReservation;
    this.authorization = authorization;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public DistributionPlan handle(ModifyDistributionPlanCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    DistributionPlan plan =
        DistributionPlans.load(
            distributionPlanRepository, command.planId(), command.organizationId());
    plan.modifyItems(
        command.changes(), command.authorityId(), command.reason(), LocalDateTime.now(clock));
    resourceReservation.change(plan.id(), plan.distributedQuantities());
    DistributionPlan saved = distributionPlanRepository.save(plan);
    eventPublisher.publish(new DistributionPlanModified(saved.id(), command.authorityId()));
    return saved;
  }
}
