package com.waqtalabs.auxia.resourcemanagement.application.queryservices;

import com.waqtalabs.auxia.resourcemanagement.application.queries.GetInventoryByIdQuery;
import com.waqtalabs.auxia.resourcemanagement.application.queries.GetInventoryByOrganizationIdQuery;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.InventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.OrganizationInventoryNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The inventory with the available and reserved quantity of each resource (US-09). */
@Service
@Transactional(readOnly = true)
public class InventoryQueryService {

  private final InventoryRepository inventoryRepository;

  public InventoryQueryService(InventoryRepository inventoryRepository) {
    this.inventoryRepository = inventoryRepository;
  }

  public Inventory handle(GetInventoryByIdQuery query) {
    return inventoryRepository
        .findById(query.inventoryId())
        .filter(inventory -> inventory.belongsTo(query.organizationId()))
        .orElseThrow(() -> new InventoryNotFoundException(query.inventoryId()));
  }

  public Inventory handle(GetInventoryByOrganizationIdQuery query) {
    return inventoryRepository
        .findByOrganizationId(query.organizationId())
        .orElseThrow(() -> new OrganizationInventoryNotFoundException(query.organizationId()));
  }
}
