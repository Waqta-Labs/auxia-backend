package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "traceability", name = "delivery_items")
class DeliveryItemJpaEntity {

  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_id")
  DeliveryJpaEntity delivery;

  @Column(name = "resource_id")
  UUID resourceId;

  int quantity;

  protected DeliveryItemJpaEntity() {}
}
