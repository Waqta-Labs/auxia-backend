package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.ReserveResourcesCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.OrganizationInventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Holds the stock of a proposed distribution plan in the inventory of its organization. Every
 * line is reserved or none is.
 */
@Service
public class ReserveResourcesCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final Clock clock;

  public ReserveResourcesCommandHandler(InventoryRepository inventoryRepository, Clock clock) {
    this.inventoryRepository = inventoryRepository;
    this.clock = clock;
  }

  @Transactional
  public Inventory handle(ReserveResourcesCommand command) {
    Inventory inventory =
        inventoryRepository
            .findByOrganizationId(command.organizationId())
            .orElseThrow(
                () -> new OrganizationInventoryNotFoundException(command.organizationId()));
    inventory.reserve(command.distributionPlanId(), command.lines(), LocalDateTime.now(clock));
    return inventoryRepository.save(inventory);
  }
}
