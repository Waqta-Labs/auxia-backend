package com.waqtalabs.auxia.emergencymanagement.application.eventhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.ports.PlanApprovalNotifier;
import com.waqtalabs.auxia.emergencymanagement.application.ports.PlanApprovalNotifier.ApprovedPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Notifies an approved plan once its approval is committed. A notification that fails is logged:
 * the plan stays approved.
 */
@Component
public class PlanApprovalNotificationEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(PlanApprovalNotificationEventHandler.class);

  private final DistributionPlanRepository planRepository;
  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final PlanApprovalNotifier notifier;

  public PlanApprovalNotificationEventHandler(
      DistributionPlanRepository planRepository,
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      PlanApprovalNotifier notifier) {
    this.planRepository = planRepository;
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.notifier = notifier;
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(DistributionPlanApproved event) {
    try {
      DistributionPlan plan = planRepository.findById(event.planId()).orElseThrow();
      Emergency emergency = emergencyRepository.findById(event.emergencyId()).orElseThrow();
      Zone zone = zoneRepository.findById(event.zoneId()).orElseThrow();
      notifier.notifyApproved(
          new ApprovedPlan(
              plan.id(),
              event.organizationId(),
              event.approvedBy(),
              emergency.name(),
              zone.name(),
              plan.coverage()));
    } catch (RuntimeException ex) {
      LOGGER.error("Approval of plan {} was not notified", event.planId().value(), ex);
    }
  }
}
