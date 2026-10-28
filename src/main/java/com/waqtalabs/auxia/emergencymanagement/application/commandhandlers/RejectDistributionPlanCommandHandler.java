package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RejectDistributionPlanCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.AuthorizationPort;
import com.waqtalabs.auxia.emergencymanagement.application.ports.ResourceReservationPort;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanRejected;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Rejects a proposed plan with a reason (US-14) and makes its stock available again. */
@Service
public class RejectDistributionPlanCommandHandler {

  private final DistributionPlanRepository distributionPlanRepository;
  private final ResourceReservationPort resourceReservation;
  private final AuthorizationPort authorization;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public RejectDistributionPlanCommandHandler(
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
  public DistributionPlan handle(RejectDistributionPlanCommand command) {
    DistributionPlans.requireActiveAuthority(authorization, command.authorityId());
    DistributionPlan plan =
        DistributionPlans.load(
            distributionPlanRepository, command.planId(), command.organizationId());
    plan.reject(command.authorityId(), command.reason(), LocalDateTime.now(clock));
    DistributionPlan saved = distributionPlanRepository.save(plan);
    resourceReservation.release(saved.id());
    eventPublisher.publish(
        new DistributionPlanRejected(saved.id(), saved.zoneId(), command.authorityId()));
    return saved;
  }
}
