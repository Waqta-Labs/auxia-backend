package com.waqtalabs.auxia.emergencymanagement.application.eventhandlers;

import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanModified;
import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanRejected;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportCorrected;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ShiftHandedOver;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneManualPriorityRemoved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZonePrioritizedManually;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.DistributionPlanNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.DistributionPlan;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.ZoneDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanDecision;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PlanModification;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionType;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.DistributionPlanRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneDecisionRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Keeps the history of decisions of each zone (US-20, TS-C01). It listens inside the transaction
 * of the decision, so a decision and its entry are saved together or not at all. The plan events
 * do not carry the time or the reason: they are read from the plan, saved just before.
 */
@Service
public class ZoneDecisionLogEventHandler {

  private final ZoneDecisionRepository zoneDecisionRepository;
  private final DistributionPlanRepository distributionPlanRepository;
  private final Clock clock;

  public ZoneDecisionLogEventHandler(
      ZoneDecisionRepository zoneDecisionRepository,
      DistributionPlanRepository distributionPlanRepository,
      Clock clock) {
    this.zoneDecisionRepository = zoneDecisionRepository;
    this.distributionPlanRepository = distributionPlanRepository;
    this.clock = clock;
  }

  @EventListener
  public void on(ZoneRegistered event) {
    zoneDecisionRepository.save(
        ZoneDecision.record(
            event.zoneId(),
            ZoneDecisionType.ZONE_REGISTERED,
            event.registeredBy(),
            LocalDateTime.now(clock),
            null,
            null));
  }

  @EventListener
  public void on(DistributionPlanModified event) {
    DistributionPlan plan = plan(event.planId());
    PlanModification modification = plan.modification().orElseThrow();
    zoneDecisionRepository.save(
        ZoneDecision.record(
            plan.zoneId(),
            ZoneDecisionType.PLAN_MODIFIED,
            modification.modifiedBy(),
            modification.modifiedAt(),
            modification.reason(),
            plan.id().value()));
  }

  @EventListener
  public void on(DistributionPlanApproved event) {
    record(plan(event.planId()), ZoneDecisionType.PLAN_APPROVED);
  }

  @EventListener
  public void on(DistributionPlanRejected event) {
    record(plan(event.planId()), ZoneDecisionType.PLAN_REJECTED);
  }

  @EventListener
  public void on(ZonePrioritizedManually event) {
    zoneDecisionRepository.save(
        ZoneDecision.record(
            event.zoneId(),
            ZoneDecisionType.MANUAL_PRIORITY_SET,
            event.setBy(),
            event.setAt(),
            event.reason(),
            null));
  }

  @EventListener
  public void on(ZoneManualPriorityRemoved event) {
    zoneDecisionRepository.save(
        ZoneDecision.record(
            event.zoneId(),
            ZoneDecisionType.MANUAL_PRIORITY_REMOVED,
            event.removedBy(),
            event.removedAt(),
            null,
            null));
  }

  /** A report still pending zone assignment has no zone history to add it to. */
  @EventListener
  public void on(FieldReportCorrected event) {
    if (event.zoneId() == null) {
      return;
    }
    zoneDecisionRepository.save(
        ZoneDecision.record(
            event.zoneId(),
            ZoneDecisionType.REPORT_CORRECTED,
            event.correctedBy(),
            event.correctedAt(),
            event.reason(),
            event.fieldReportId().value()));
  }

  /** One entry in the history of each zone that changed hands. */
  @EventListener
  public void on(ShiftHandedOver event) {
    for (ZoneId zoneId : event.zoneIds()) {
      zoneDecisionRepository.save(
          ZoneDecision.shiftHandedOver(
              zoneId,
              event.handoverId(),
              event.outgoing(),
              event.incoming(),
              event.handedOverAt(),
              event.notes()));
    }
  }

  private void record(DistributionPlan plan, ZoneDecisionType type) {
    PlanDecision decision = plan.decision().orElseThrow();
    zoneDecisionRepository.save(
        ZoneDecision.record(
            plan.zoneId(),
            type,
            decision.decidedBy(),
            decision.decidedAt(),
            plan.rejectedReason().orElse(null),
            plan.id().value()));
  }

  private DistributionPlan plan(DistributionPlanId planId) {
    return distributionPlanRepository
        .findById(planId)
        .orElseThrow(() -> new DistributionPlanNotFoundException(planId));
  }
}
