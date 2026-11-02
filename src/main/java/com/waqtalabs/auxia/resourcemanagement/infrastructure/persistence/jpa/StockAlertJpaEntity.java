package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StockAlertStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "resource_management", name = "stock_alerts")
class StockAlertJpaEntity {

  @Id UUID id;

  @Column(name = "inventory_id")
  UUID inventoryId;

  @Column(name = "resource_id")
  UUID resourceId;

  @Column(name = "resource_name")
  String resourceName;

  @Column(name = "available_quantity")
  int availableQuantity;

  @Column(name = "minimum_stock")
  int minimumStock;

  @Enumerated(EnumType.STRING)
  StockAlertStatus status;

  @Column(name = "raised_at")
  LocalDateTime raisedAt;

  @Column(name = "resolved_at")
  LocalDateTime resolvedAt;

  protected StockAlertJpaEntity() {}
}
