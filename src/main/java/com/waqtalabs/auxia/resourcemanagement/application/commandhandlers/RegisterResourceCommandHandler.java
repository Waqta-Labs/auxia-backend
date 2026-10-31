package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.RegisterResourceCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.InventoryStockChanged;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockItem;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterResourceCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final DomainEventPublisher eventPublisher;

  public RegisterResourceCommandHandler(
      InventoryRepository inventoryRepository, DomainEventPublisher eventPublisher) {
    this.inventoryRepository = inventoryRepository;
    this.eventPublisher = eventPublisher;
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
    Inventory saved = inventoryRepository.save(inventory);
    eventPublisher.publish(new InventoryStockChanged(saved.id()));
    return saved.stockItem(resourceId);
  }
}
