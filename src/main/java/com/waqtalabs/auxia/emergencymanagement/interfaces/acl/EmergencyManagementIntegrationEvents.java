package com.waqtalabs.auxia.emergencymanagement.interfaces.acl;

import com.waqtalabs.auxia.emergencymanagement.domain.events.DistributionPlanApproved;
import com.waqtalabs.auxia.emergencymanagement.domain.events.EmergencyRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.events.UrgencyScoreCalculated;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneRegistered;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates the domain events that other contexts need into their public versions. It runs
 * while the event is published, inside the transaction of the command, so the consumers can wait
 * for the commit with {@code @TransactionalEventListener}.
 */
@Component
class EmergencyManagementIntegrationEvents {

  private final ApplicationEventPublisher publisher;

  EmergencyManagementIntegrationEvents(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @EventListener
  void on(EmergencyRegistered event) {
    publisher.publishEvent(
        new EmergencyRegisteredIntegrationEvent(
            event.emergencyId().value(),
            event.name(),
            event.type().name(),
            event.location().department(),
            event.location().province(),
            event.location().district(),
            event.startDate()));
  }

  @EventListener
  void on(ZoneRegistered event) {
    publisher.publishEvent(
        new ZoneRegisteredIntegrationEvent(
            event.zoneId().value(), event.emergencyId().value(), event.name()));
  }

  @EventListener
  void on(UrgencyScoreCalculated event) {
    publisher.publishEvent(new ZonePrioritizedIntegrationEvent(event.zoneId().value()));
  }

  @EventListener
  void on(DistributionPlanApproved event) {
    publisher.publishEvent(
        new DistributionPlanApprovedIntegrationEvent(
            event.planId().value(), event.zoneId().value()));
  }
}
