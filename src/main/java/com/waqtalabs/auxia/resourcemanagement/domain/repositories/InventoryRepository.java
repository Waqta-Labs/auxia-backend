package com.waqtalabs.auxia.resourcemanagement.domain.repositories;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import java.util.Optional;

public interface InventoryRepository {

  Inventory save(Inventory inventory);

  Optional<Inventory> findById(InventoryId id);

  Optional<Inventory> findByOrganizationId(OrganizationId organizationId);

  /** The inventory that holds the reservation of a distribution plan. */
  Optional<Inventory> findByReservationPlanId(DistributionPlanId planId);
}
