package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.RegisterResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterResourceCommandHandler {

  private final InventoryRepository inventoryRepository;

  public RegisterResourceCommandHandler(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  @Transactional
  public StockItem handle(RegisterResourceCommand command) {
    Inventory inventory =
        inventoryRepository
            .findById(command.inventoryId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new InventoryNotFoundException(command.inventoryId()));
    ResourceId resourceId =
        inventory.registerResource(
            command.name(), command.category(), command.unit(), command.minimumStock());
    return inventoryRepository.save(inventory).stockItem(resourceId);
  }
}
