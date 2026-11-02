package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AdjustStockCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.InventoryStockChanged;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
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
  private final DomainEventPublisher eventPublisher;

  public AdjustStockCommandHandler(
      InventoryRepository inventoryRepository, DomainEventPublisher eventPublisher) {
    this.inventoryRepository = inventoryRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public StockItem handle(AdjustStockCommand command) {
    Inventory inventory =
        inventoryRepository
            .findById(command.inventoryId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new InventoryNotFoundException(command.inventoryId()));
    inventory.adjustStock(command.resourceId(), command.quantityChange(), command.reason());
    Inventory saved = inventoryRepository.save(inventory);
    eventPublisher.publish(new InventoryStockChanged(saved.id()));
    StockItem item = saved.stockItem(command.resourceId());
    LOGGER.info(
        "Stock of resource {} adjusted by {} to {}: {}",
        command.resourceId().value(),
        command.quantityChange(),
        item.stockLevel().onHand(),
        command.reason());
    return item;
  }
}
