package com.waqtalabs.auxia.resourcemanagement.application.eventhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commandhandlers.EvaluateStockAlertsCommandHandler;
import com.waqtalabs.auxia.resourcemanagement.application.commands.EvaluateStockAlertsCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.InventoryStockChanged;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Evaluates the low stock alerts once a change of stock is committed, in a transaction of its
 * own: a failure here never undoes the change.
 */
@Component
public class InventoryStockChangedEventHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(InventoryStockChangedEventHandler.class);

  private final EvaluateStockAlertsCommandHandler evaluateStockAlerts;
  private final TransactionTemplate newTransaction;

  public InventoryStockChangedEventHandler(
      EvaluateStockAlertsCommandHandler evaluateStockAlerts,
      PlatformTransactionManager transactionManager) {
    this.evaluateStockAlerts = evaluateStockAlerts;
    this.newTransaction = new TransactionTemplate(transactionManager);
    newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
  }

  @TransactionalEventListener(fallbackExecution = true)
  public void on(InventoryStockChanged event) {
    var command = new EvaluateStockAlertsCommand(event.inventoryId());
    try {
      newTransaction.executeWithoutResult(status -> evaluateStockAlerts.handle(command));
    } catch (RuntimeException ex) {
      LOGGER.error(
          "Stock alerts of inventory {} were not evaluated", event.inventoryId().value(), ex);
    }
  }
}
