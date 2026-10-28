package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.ApproveDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Approves a proposed plan under the responsibility of an authority (US-12). Its stock stays
 * reserved until the delivery consumes it.
 */
@Service
public class ApproveDistributionPlanCommandHandler {

  private final DistributionPlanRepository distributionPlanRepository;
  private final AuthorizationPort authorization;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public ApproveDistributionPlanCommandHandler(
      DistributionPlanRepository distributionPlanRepository,
      AuthorizationPort authorization,
      DomainEventPublisher eventPublisher,
      Clock clock) {
    this.distributionPlanRepository = distributionPlanRepository;
    this.authorization = authorization;
    this.eventPublisher = eventPublisher;
    this.clock = clock;
  }

  @Transactional
  public DistributionPlan handle(ApproveDistributionPlanCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    DistributionPlan plan =
        DistributionPlans.load(
            distributionPlanRepository, command.planId(), command.organizationId());
    plan.approve(command.authorityId(), LocalDateTime.now(clock));
    DistributionPlan saved = distributionPlanRepository.save(plan);
    eventPublisher.publish(
        new DistributionPlanApproved(
            saved.id(),
            saved.emergencyId(),
            saved.zoneId(),
            saved.organizationId(),
            command.authorityId()));
    return saved;
  }
}
