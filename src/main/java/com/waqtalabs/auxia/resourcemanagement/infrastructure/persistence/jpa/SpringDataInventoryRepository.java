package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface SpringDataInventoryRepository extends JpaRepository<InventoryJpaEntity, UUID> {

  Optional<InventoryJpaEntity> findByOrganizationId(UUID organizationId);

  @Query(
      "select inventory from InventoryJpaEntity inventory join inventory.reservations reservation"
          + " where reservation.distributionPlanId = :planId")
  Optional<InventoryJpaEntity> findByReservationPlanId(UUID planId);
}
