package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.ReleaseReservationCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.ReservationNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseReservationCommandHandler {

  private final InventoryRepository inventoryRepository;
  private final Clock clock;

  public ReleaseReservationCommandHandler(InventoryRepository inventoryRepository, Clock clock) {
    this.inventoryRepository = inventoryRepository;
    this.clock = clock;
  }

  @Transactional
  public Inventory handle(ReleaseReservationCommand command) {
    Inventory inventory =
        inventoryRepository
            .findByReservationPlanId(command.distributionPlanId())
            .orElseThrow(() -> new ReservationNotFoundException(command.distributionPlanId()));
    inventory.releaseReservation(command.distributionPlanId(), LocalDateTime.now(clock));
    return inventoryRepository.save(inventory);
  }
}
