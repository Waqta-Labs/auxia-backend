package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.ChangeReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.events.InventoryStockChanged;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeReservationCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final DomainEventPublisher eventPublisher;

  public ChangeReservationCommandHandler(
      InventoryRepository inventoryRepository, DomainEventPublisher eventPublisher) {
    this.inventoryRepository = inventoryRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Inventory handle(ChangeReservationCommand command) {
    Inventory inventory =
        inventoryRepository
            .findByReservationPlanId(command.distributionPlanId())
            .orElseThrow(() -> new ReservationNotFoundException(command.distributionPlanId()));
    inventory.changeReservation(command.distributionPlanId(), command.lines());
    Inventory saved = inventoryRepository.save(inventory);
    eventPublisher.publish(new InventoryStockChanged(saved.id()));
    return saved;
  }
}
