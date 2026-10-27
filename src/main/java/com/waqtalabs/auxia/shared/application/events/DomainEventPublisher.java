package com.waqtalabs.auxia.shared.application.events;

import com.waqtalabs.auxia.shared.domain.events.DomainEvent;

/**
 * Outgoing port to publish domain events. The command handlers publish after saving, and the
 * listeners receive the event once the transaction is committed.
 */
@FunctionalInterface
public interface DomainEventPublisher {

  void publish(DomainEvent event);
}
