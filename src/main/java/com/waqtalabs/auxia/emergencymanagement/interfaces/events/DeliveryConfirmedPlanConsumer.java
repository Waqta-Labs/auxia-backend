package com.waqtalabs.auxia.emergencymanagement.interfaces.events;

import com.waqtalabs.auxia.emergencymanagement.application.commandhandlers.RecordAidDeliveredCommandHandler;
import com.waqtalabs.auxia.emergencymanagement.application.commands.RecordAidDeliveredCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.interfaces.acl.DeliveryConfirmedIntegrationEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Marks the plan executed and its zone served once Traceability commits a delivery. It runs in a
 * transaction of its own, so a failure here never undoes the registration of the delivery.
 */
@Component
class DeliveryConfirmedPlanConsumer {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DeliveryConfirmedPlanConsumer.class);

  private final RecordAidDeliveredCommandHandler recordAidDelivered;
  private final TransactionTemplate newTransaction;

  DeliveryConfirmedPlanConsumer(
      RecordAidDeliveredCommandHandler recordAidDelivered,
      PlatformTransactionManager transactionManager) {
    this.recordAidDelivered = recordAidDelivered;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(DeliveryConfirmedIntegrationEvent event) {
    var command =
        new RecordAidDeliveredCommand(
            new DistributionPlanId(event.distributionPlanId()), event.deliveredAt());
    try {
      newTransaction.executeWithoutResult(status -> recordAidDelivered.handle(command));
    } catch (RuntimeException ex) {
      LOGGER.error("Plan {} could not be marked executed", event.distributionPlanId(), ex);
    }
  }
}
