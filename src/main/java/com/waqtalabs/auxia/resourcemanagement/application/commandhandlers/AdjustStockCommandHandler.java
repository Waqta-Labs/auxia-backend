package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AdjustStockCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Corrects the physical stock of a resource with a reason (TS-03). Until the audit trail exists
 * (QAD-03), every adjustment and its reason are written to the log.
 */
@Service
public class AdjustStockCommandHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(AdjustStockCommandHandler.class);

  private final InventoryRepository inventoryRepository;

  public AdjustStockCommandHandler(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  @Transactional
  public StockItem handle(AdjustStockCommand command) {
    Inventory inventory =
        inventoryRepository
            .findById(command.inventoryId())
            .orElseThrow(() -> new InventoryNotFoundException(command.inventoryId()));
    inventory.adjustStock(command.resourceId(), command.quantityChange(), command.reason());
    StockItem item = inventoryRepository.save(inventory).stockItem(command.resourceId());
    LOGGER.info(
        "Stock of resource {} adjusted by {} to {}: {}",
        command.resourceId().value(),
        command.quantityChange(),
        item.stockLevel().onHand(),
        command.reason());
    return item;
  }
}
