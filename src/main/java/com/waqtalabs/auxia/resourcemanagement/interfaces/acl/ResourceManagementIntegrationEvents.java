package com.waqtalabs.auxia.resourcemanagement.interfaces.acl;

import com.waqtalabs.auxia.resourcemanagement.domain.events.BrigadeAssigned;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates the domain events that other contexts need into their public versions. It runs
 * while the event is published, inside the transaction of the command, so the consumers can wait
 * for the commit with {@code @TransactionalEventListener}.
 */
@Component
class ResourceManagementIntegrationEvents {

  private final ApplicationEventPublisher publisher;

  ResourceManagementIntegrationEvents(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @EventListener
  void on(BrigadeAssigned event) {
    publisher.publishEvent(
        new BrigadeAssignedIntegrationEvent(
            event.brigadeId().value(),
            event.distributionPlanId().value(),
            event.zoneId().value(),
            event.organizationId().value(),
            event.assignedAt()));
  }
}
