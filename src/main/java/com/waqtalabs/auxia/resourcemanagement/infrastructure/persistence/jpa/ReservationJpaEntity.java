package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.ReservationStatus;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Row of {@code resource_management.reservations} with its lines. */
@Entity
@Table(schema = "resource_management", name = "reservations")
class ReservationJpaEntity {

  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "inventory_id")
  InventoryJpaEntity inventory;

  @Column(name = "distribution_plan_id")
  UUID distributionPlanId;

  @Enumerated(EnumType.STRING)
  ReservationStatus status;

  @Column(name = "reserved_at")
  LocalDateTime reservedAt;

  @Column(name = "closed_at")
  LocalDateTime closedAt;

  @ElementCollection
  @CollectionTable(
      schema = "resource_management",
      name = "reservation_lines",
      joinColumns = @JoinColumn(name = "reservation_id"))
  List<ReservationLineEmbeddable> lines = new ArrayList<>();

  protected ReservationJpaEntity() {}
}
