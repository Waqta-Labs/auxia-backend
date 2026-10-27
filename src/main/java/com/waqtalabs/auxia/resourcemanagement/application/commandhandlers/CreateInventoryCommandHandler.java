package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.CreateInventoryCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryAlreadyExistsException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Each organization has a single inventory (TS-C08). */
@Service
public class CreateInventoryCommandHandler {

  private final InventoryRepository inventoryRepository;

  public CreateInventoryCommandHandler(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  @Transactional
  public Inventory handle(CreateInventoryCommand command) {
    if (inventoryRepository.findByOrganizationId(command.organizationId()).isPresent()) {
      throw new InventoryAlreadyExistsException(command.organizationId());
    }
    return inventoryRepository.save(Inventory.create(command.organizationId(), command.name()));
  }
}
