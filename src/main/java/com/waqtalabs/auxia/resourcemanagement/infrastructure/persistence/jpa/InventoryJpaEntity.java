package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Row of {@code resource_management.inventories} with its stock items and reservations. Only
 * {@link InventoryJpaMapper} uses it.
 */
@Entity
@Table(schema = "resource_management", name = "inventories")
class InventoryJpaEntity {

  @Id UUID id;

  @Column(name = "organization_id")
  UUID organizationId;

  String name;

  @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("name")
  List<StockItemJpaEntity> stockItems = new ArrayList<>();

  @OneToMany(mappedBy = "inventory", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("reservedAt")
  List<ReservationJpaEntity> reservations = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected InventoryJpaEntity() {}
}
