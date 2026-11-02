package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Inventory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.DistributionPlanId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.InventoryRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * The stock items, reservations and reservation lines are loaded lazily, so every method runs in
 * a transaction while the mapper reads them.
 */
@Repository
class JpaInventoryRepository implements InventoryRepository {

  private final SpringDataInventoryRepository repository;

  JpaInventoryRepository(SpringDataInventoryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public Inventory save(Inventory inventory) {
    InventoryJpaEntity entity =
        repository.findById(inventory.id().value()).orElseGet(InventoryJpaEntity::new);
    InventoryJpaMapper.copy(inventory, entity);
    return InventoryJpaMapper.toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Inventory> findById(InventoryId id) {
    return repository.findById(id.value()).map(InventoryJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Inventory> findByOrganizationId(OrganizationId organizationId) {
    return repository
        .findByOrganizationId(organizationId.value())
        .map(InventoryJpaMapper::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Inventory> findByReservationPlanId(DistributionPlanId planId) {
    return repository.findByReservationPlanId(planId.value()).map(InventoryJpaMapper::toDomain);
  }
}
