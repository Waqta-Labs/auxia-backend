package com.waqtalabs.auxia.traceability.interfaces.events;

import com.waqtalabs.auxia.resourcemanagement.interfaces.acl.BrigadeAssignedIntegrationEvent;
import com.waqtalabs.auxia.traceability.application.commandhandlers.InitiateDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commands.InitiateDeliveryCommand;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.BrigadeId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.ZoneId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Opens the delivery of a plan once Resource Management commits the assignment of its brigade.
 * It runs in a transaction of its own, so a failure here never undoes the assignment.
 */
@Component
class BrigadeAssignedEventConsumer {

  private static final Logger LOGGER = LoggerFactory.getLogger(BrigadeAssignedEventConsumer.class);

  private final InitiateDeliveryCommandHandler initiateDelivery;
  private final TransactionTemplate newTransaction;

  BrigadeAssignedEventConsumer(
      InitiateDeliveryCommandHandler initiateDelivery,
      PlatformTransactionManager transactionManager) {
    this.initiateDelivery = initiateDelivery;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  void on(BrigadeAssignedIntegrationEvent event) {
    var command =
        new InitiateDeliveryCommand(
            new DistributionPlanId(event.distributionPlanId()),
            new ZoneId(event.zoneId()),
            new BrigadeId(event.brigadeId()),
            new OrganizationId(event.organizationId()));
    try {
      newTransaction.executeWithoutResult(status -> initiateDelivery.handle(command));
    } catch (RuntimeException ex) {
      LOGGER.error("The delivery of plan {} could not be opened", event.distributionPlanId(), ex);
    }
  }
}
