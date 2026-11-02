package com.waqtalabs.auxia.resourcemanagement.infrastructure.notifications;

import com.waqtalabs.auxia.resourcemanagement.application.ports.StockAlertNotifier;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes each new alert to the log. The alerts are read with {@code GET
 * /inventories/{id}/alerts}; an e-mail or another channel replaces this adapter without touching
 * the use case.
 */
@Component
class LoggingStockAlertNotifier implements StockAlertNotifier {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingStockAlertNotifier.class);

  @Override
  public void notify(StockAlert alert) {
    LOGGER.warn(
        "Low stock in inventory {}: {} has {} available, below its minimum of {}",
        alert.inventoryId().value(),
        alert.resourceName(),
        alert.availableQuantity(),
        alert.minimumStock());
  }
}
