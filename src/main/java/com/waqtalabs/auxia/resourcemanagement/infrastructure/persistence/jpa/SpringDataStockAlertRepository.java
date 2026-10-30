package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface SpringDataStockAlertRepository extends JpaRepository<StockAlertJpaEntity, UUID> {

  List<StockAlertJpaEntity> findByInventoryIdOrderByRaisedAtDesc(UUID inventoryId);

  List<StockAlertJpaEntity> findByInventoryIdAndStatusOrderByRaisedAtDesc(
      UUID inventoryId, StockAlertStatus status);
}
