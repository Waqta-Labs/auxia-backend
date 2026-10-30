package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.ReserveResourcesCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.InventoryStockChanged;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.OrganizationInventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
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
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public ReserveResourcesCommandHandler(
      InventoryRepository inventoryRepository, DomainEventPublisher eventPublisher, Clock clock) {
    this.inventoryRepository = inventoryRepository;
    this.eventPublisher = eventPublisher;
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
    Inventory saved = inventoryRepository.save(inventory);
    eventPublisher.publish(new InventoryStockChanged(saved.id()));
    return saved;
  }
}
