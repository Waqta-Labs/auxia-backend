package com.waqtalabs.auxia.traceability.application.eventhandlers;

import com.waqtalabs.auxia.traceability.application.commandhandlers.AnchorDeliveryCommandHandler;
import com.waqtalabs.auxia.traceability.application.commands.AnchorDeliveryCommand;
import com.waqtalabs.auxia.traceability.domain.events.DeliveryConfirmed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Anchors the hash of every delivery once its registration is committed (US-16).
 *
 * <p>It runs on the anchoring executor, apart from the request, because a block of the
 * blockchain can take tens of seconds: the brigade gets its answer without waiting, and the
 * registration is never undone if the network fails. The committed transaction of the
 * registration is suspended, so each change of the anchoring is saved in a transaction of its
 * own even when the executor runs it in the same thread. It is the last reaction to the
 * confirmation, so the other contexts see the delivery before its certification.
 */
@Component
public class DeliveryConfirmedEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(DeliveryConfirmedEventHandler.class);

  private final AnchorDeliveryCommandHandler anchorDelivery;
  private final TransactionTemplate withoutTransaction;

  public DeliveryConfirmedEventHandler(
      AnchorDeliveryCommandHandler anchorDelivery, PlatformTransactionManager transactionManager) {
    this.anchorDelivery = anchorDelivery;
    this.withoutTransaction = new TransactionTemplate(transactionManager);
    withoutTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
  }

  @Async("anchoringExecutor")
  @Order(Ordered.LOWEST_PRECEDENCE)
  @TransactionalEventListener(fallbackExecution = true)
  public void on(DeliveryConfirmed event) {
    try {
      withoutTransaction.executeWithoutResult(
          status -> anchorDelivery.handle(new AnchorDeliveryCommand(event.deliveryId())));
    } catch (RuntimeException ex) {
      LOGGER.error("Delivery {} could not be anchored", event.deliveryId().value(), ex);
    }
  }
}
