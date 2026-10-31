package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StockAlert;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.InventoryId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertId;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.StockAlertRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class JpaStockAlertRepository implements StockAlertRepository {

  private final SpringDataStockAlertRepository repository;

  JpaStockAlertRepository(SpringDataStockAlertRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public StockAlert save(StockAlert alert) {
    StockAlertJpaEntity entity =
        repository.findById(alert.id().value()).orElseGet(StockAlertJpaEntity::new);
    entity.id = alert.id().value();
    entity.inventoryId = alert.inventoryId().value();
    entity.resourceId = alert.resourceId().value();
    entity.resourceName = alert.resourceName();
    entity.availableQuantity = alert.availableQuantity();
    entity.minimumStock = alert.minimumStock();
    entity.status = alert.status();
    entity.raisedAt = alert.raisedAt();
    entity.resolvedAt = alert.resolvedAt().orElse(null);
    return toDomain(repository.save(entity));
  }

  @Override
  @Transactional(readOnly = true)
  public List<StockAlert> findByInventoryId(InventoryId inventoryId, StockAlertStatus status) {
    List<StockAlertJpaEntity> found =
        status == null
            ? repository.findByInventoryIdOrderByRaisedAtDesc(inventoryId.value())
            : repository.findByInventoryIdAndStatusOrderByRaisedAtDesc(inventoryId.value(), status);
    return found.stream().map(JpaStockAlertRepository::toDomain).toList();
  }

  private static StockAlert toDomain(StockAlertJpaEntity entity) {
    return StockAlert.reconstitute(
        new StockAlertId(entity.id),
        new InventoryId(entity.inventoryId),
        new ResourceId(entity.resourceId),
        entity.resourceName,
        entity.availableQuantity,
        entity.minimumStock,
        entity.status,
        entity.raisedAt,
        entity.resolvedAt);
  }
}
