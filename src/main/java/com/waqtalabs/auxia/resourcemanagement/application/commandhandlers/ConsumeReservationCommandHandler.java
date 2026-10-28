package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.ConsumeReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Takes the delivered stock of a plan out of the warehouse and makes what was not delivered
 * available again.
 */
@Service
public class ConsumeReservationCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final Clock clock;

  public ConsumeReservationCommandHandler(InventoryRepository inventoryRepository, Clock clock) {
    this.inventoryRepository = inventoryRepository;
    this.clock = clock;
  }

  @Transactional
  public Inventory handle(ConsumeReservationCommand command) {
    Inventory inventory =
        inventoryRepository
            .findByReservationPlanId(command.distributionPlanId())
            .orElseThrow(() -> new ReservationNotFoundException(command.distributionPlanId()));
    inventory.consumeReservation(
        command.distributionPlanId(), command.delivered(), LocalDateTime.now(clock));
    return inventoryRepository.save(inventory);
  }
}
