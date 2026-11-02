package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(schema = "emergency_management", name = "distribution_items")
class DistributionItemJpaEntity {

  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "distribution_plan_id")
  DistributionPlanJpaEntity plan;

  @Column(name = "resource_id")
  UUID resourceId;

  @Column(name = "recommended_quantity")
  Integer recommendedQuantity;

  int quantity;

  protected DistributionItemJpaEntity() {}
}
