package com.waqtalabs.auxia.traceability.interfaces.acl;

import com.waqtalabs.auxia.traceability.domain.events.DeliveryConfirmed;
import com.waqtalabs.auxia.traceability.interfaces.acl.DeliveryConfirmedIntegrationEvent.DeliveredItem;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Translates the domain events that other contexts need into their public versions. It runs
 * while the event is published, inside the transaction of the command, so the consumers can wait
 * for the commit with {@code @TransactionalEventListener}.
 */
@Component
class TraceabilityIntegrationEvents {

  private final ApplicationEventPublisher publisher;

  TraceabilityIntegrationEvents(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @EventListener
  void on(DeliveryConfirmed event) {
    publisher.publishEvent(
        new DeliveryConfirmedIntegrationEvent(
            event.deliveryId().value(),
            event.distributionPlanId().value(),
            event.zoneId().value(),
            event.brigadeId().value(),
            event.organizationId().value(),
            event.items().stream()
                .map(line -> new DeliveredItem(line.resourceId().value(), line.quantity()))
                .toList(),
            event.deliveredAt()));
  }
}
