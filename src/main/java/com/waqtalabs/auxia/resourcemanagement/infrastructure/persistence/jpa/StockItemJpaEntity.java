package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ResourceCategory;
import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.UnitOfMeasure;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.UpdateTimestamp;

/** Row of {@code resource_management.stock_items}. */
@Entity
@Table(schema = "resource_management", name = "stock_items")
class StockItemJpaEntity {

  @Id
  @Column(name = "resource_id")
  UUID resourceId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "inventory_id")
  InventoryJpaEntity inventory;

  String name;

  @Enumerated(EnumType.STRING)
  ResourceCategory category;

  @Enumerated(EnumType.STRING)
  UnitOfMeasure unit;

  @Column(name = "on_hand")
  int onHand;

  int reserved;

  @Column(name = "minimum_stock")
  int minimumStock;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected StockItemJpaEntity() {}
}
