package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.EvaluateStockAlertsCommand;
import com.waqtalabs.auxia.resourcemanagement.application.ports.StockAlertNotifier;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.StockAlertRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Raises an alert for each resource that fell below its minimum stock, and resolves the open
 * alert of each resource that is above it again (US-10). A resource has one open alert at most.
 */
@Service
public class EvaluateStockAlertsCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final StockAlertRepository stockAlertRepository;
  private final StockAlertNotifier notifier;
  private final Clock clock;

  public EvaluateStockAlertsCommandHandler(
      InventoryRepository inventoryRepository,
      StockAlertRepository stockAlertRepository,
      StockAlertNotifier notifier,
      Clock clock) {
    this.inventoryRepository = inventoryRepository;
    this.stockAlertRepository = stockAlertRepository;
    this.notifier = notifier;
    this.clock = clock;
  }

  @Transactional
  public void handle(EvaluateStockAlertsCommand command) {
    Inventory inventory = inventoryRepository.findById(command.inventoryId()).orElse(null);
    if (inventory == null) {
      return;
    }
    Map<ResourceId, StockAlert> open =
        stockAlertRepository.findByInventoryId(inventory.id(), StockAlertStatus.OPEN).stream()
            .collect(Collectors.toMap(StockAlert::resourceId, Function.identity()));
    LocalDateTime now = LocalDateTime.now(clock);
    for (StockItem item : inventory.stockItems()) {
      StockAlert alert = open.get(item.resourceId());
      if (item.isBelowMinimum() && alert == null) {
        notifier.notify(stockAlertRepository.save(StockAlert.raise(inventory.id(), item, now)));
      } else if (!item.isBelowMinimum() && alert != null) {
        alert.resolve(now);
        stockAlertRepository.save(alert);
      }
    }
  }
}
