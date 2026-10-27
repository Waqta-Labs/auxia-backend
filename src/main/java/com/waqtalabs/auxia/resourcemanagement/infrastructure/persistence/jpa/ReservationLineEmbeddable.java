package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.util.UUID;

/**
 * Row of {@code resource_management.reservation_lines}. It repeats the inventory of its
 * reservation because the composite foreign keys of the table use it: a reservation cannot hold
 * a resource of another inventory.
 */
@Embeddable
class ReservationLineEmbeddable {

  @Column(name = "inventory_id")
  UUID inventoryId;

  @Column(name = "resource_id")
  UUID resourceId;

  int quantity;

  protected ReservationLineEmbeddable() {}

  ReservationLineEmbeddable(UUID inventoryId, UUID resourceId, int quantity) {
    this.inventoryId = inventoryId;
    this.resourceId = resourceId;
    this.quantity = quantity;
  }
}
