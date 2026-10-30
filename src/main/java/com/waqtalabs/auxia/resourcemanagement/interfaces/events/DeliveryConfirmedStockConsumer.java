package com.waqtalabs.auxia.resourcemanagement.interfaces.events;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.CompleteBrigadeAssignmentCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.ConsumeReservationCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.CompleteBrigadeAssignmentCommand;
import com.waqtalabs.auxia.resourcemanagement.application.commands.ConsumeReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationLine;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.traceability.interfaces.acl.DeliveryConfirmedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Once Traceability commits a delivery, its stock leaves the warehouse and its brigade is free
 * again, both in one transaction of their own: a failure here never undoes the delivery.
 */
@Component
class DeliveryConfirmedStockConsumer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DeliveryConfirmedStockConsumer.class);

  private final ConsumeReservationCommandHandler consumeReservation;
  private final CompleteBrigadeAssignmentCommandHandler completeAssignment;
  private final TransactionTemplate newTransaction;

  DeliveryConfirmedStockConsumer(
      ConsumeReservationCommandHandler consumeReservation,
      CompleteBrigadeAssignmentCommandHandler completeAssignment,
      PlatformTransactionManager transactionManager) {
    this.consumeReservation = consumeReservation;
    this.completeAssignment = completeAssignment;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(DeliveryConfirmedIntegrationEvent event) {
    var plan = new DistributionPlanId(event.distributionPlanId());
    var delivered =
        event.items().stream()
            .map(item -> new ReservationLine(new ResourceId(item.resourceId()), item.quantity()))
            .toList();
    try {
      newTransaction.executeWithoutResult(
          status -> {
            consumeReservation.handle(new ConsumeReservationCommand(plan, delivered));
            completeAssignment.handle(new CompleteBrigadeAssignmentCommand(plan));
          });
    } catch (RuntimeException ex) {
      LOGGER.error("The stock and brigade of plan {} were not released", plan.value(), ex);
    }
  }
}
