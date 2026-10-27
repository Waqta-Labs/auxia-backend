package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.RestockResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers the entry of received resources into the inventory (US-08). */
@Service
public class RestockResourceCommandHandler {

  private final InventoryRepository inventoryRepository;

  public RestockResourceCommandHandler(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  @Transactional
  public StockItem handle(RestockResourceCommand command) {
    Inventory inventory =
        inventoryRepository
            .findById(command.inventoryId())
            .orElseThrow(() -> new InventoryNotFoundException(command.inventoryId()));
    inventory.restock(command.resourceId(), command.quantity());
    return inventoryRepository.save(inventory).stockItem(command.resourceId());
  }
}
