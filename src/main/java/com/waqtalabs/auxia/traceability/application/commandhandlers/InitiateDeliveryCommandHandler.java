package com.waqtalabs.auxia.traceability.application.commandhandlers;

import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import com.waqtalabs.auxia.traceability.application.commands.InitiateDeliveryCommand;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryInitiated;
import com.waqtalabs.auxia.traceability.domain.model.aggregates.Delivery;
import com.waqtalabs.auxia.traceability.domain.repositories.DeliveryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Opens the delivery of a plan once its brigade is assigned. A plan has one delivery. */
@Service
public class InitiateDeliveryCommandHandler {

  private final DeliveryRepository deliveryRepository;
  private final DomainEventPublisher eventPublisher;

  public InitiateDeliveryCommandHandler(
      DeliveryRepository deliveryRepository, DomainEventPublisher eventPublisher) {
    this.deliveryRepository = deliveryRepository;
    this.eventPublisher = eventPublisher;
  }

  /** The new delivery, or the one the plan already has if the event arrives twice. */
  @Transactional
  public Delivery handle(InitiateDeliveryCommand command) {
    var existing = deliveryRepository.findByDistributionPlanId(command.distributionPlanId());
    if (existing.isPresent()) {
      return existing.get();
    }
    Delivery delivery =
        deliveryRepository.save(
            Delivery.initiate(
                command.distributionPlanId(),
                command.zoneId(),
                command.brigadeId(),
                command.organizationId()));
    eventPublisher.publish(
        new DeliveryInitiated(
            delivery.id(),
            delivery.distributionPlanId(),
            delivery.zoneId(),
            delivery.brigadeId()));
    return delivery;
  }
}
