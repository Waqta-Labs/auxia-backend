package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.RestockResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.InventoryStockChanged;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers the entry of received resources into the inventory (US-08). */
@Service
public class RestockResourceCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final DomainEventPublisher eventPublisher;

  public RestockResourceCommandHandler(
      InventoryRepository inventoryRepository, DomainEventPublisher eventPublisher) {
    this.inventoryRepository = inventoryRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public StockItem handle(RestockResourceCommand command) {
    Inventory inventory =
        inventoryRepository
            .findById(command.inventoryId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new InventoryNotFoundException(command.inventoryId()));
    inventory.restock(command.resourceId(), command.quantity());
    Inventory saved = inventoryRepository.save(inventory);
    eventPublisher.publish(new InventoryStockChanged(saved.id()));
    return saved.stockItem(command.resourceId());
  }
}
