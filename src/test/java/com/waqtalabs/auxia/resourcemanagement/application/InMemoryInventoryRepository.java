package com.waqtalabs.auxia.resourcemanagement.application;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository. */
public class InMemoryInventoryRepository implements InventoryRepository {

  private final Map<InventoryId, Inventory> inventories = new LinkedHashMap<>();

  @Override
  public Inventory save(Inventory inventory) {
    inventories.put(inventory.id(), inventory);
    return inventory;
  }

  @Override
  public Optional<Inventory> findById(InventoryId id) {
    return Optional.ofNullable(inventories.get(id));
  }

  @Override
  public Optional<Inventory> findByOrganizationId(OrganizationId organizationId) {
    return inventories.values().stream()
        .filter(inventory -> inventory.belongsTo(organizationId))
        .findFirst();
  }

  @Override
  public Optional<Inventory> findByReservationPlanId(DistributionPlanId planId) {
    return inventories.values().stream()
        .filter(
            inventory ->
                inventory.reservations().stream()
                    .anyMatch(reservation -> reservation.isFor(planId)))
        .findFirst();
  }
}
