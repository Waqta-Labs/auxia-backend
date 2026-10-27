package com.waqtalabs.auxia.shared.infrastructure.events;

import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import com.waqtalabs.auxia.shared.domain.events.DomainEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * Publishes domain events inside the same process. AuxIA is a modular monolith, so it needs no
 * message broker.
 */
@Component
class SpringDomainEventPublisher implements DomainEventPublisher {

  private final ApplicationEventPublisher publisher;

  SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
    this.publisher = publisher;
  }

  @Override
  public void publish(DomainEvent event) {
    publisher.publishEvent(event);
  }
}
