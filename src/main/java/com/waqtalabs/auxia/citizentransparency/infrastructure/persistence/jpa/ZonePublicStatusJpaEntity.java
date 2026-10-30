package com.waqtalabs.auxia.citizentransparency.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.citizentransparency.domain.model.valueobjects.PublicStage;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "citizen_transparency", name = "zone_public_statuses")
class ZonePublicStatusJpaEntity {

  @Id
  @Column(name = "zone_id")
  UUID zoneId;

  @Column(name = "emergency_id")
  UUID emergencyId;

  @Column(name = "zone_name")
  String zoneName;

  @Enumerated(EnumType.STRING)
  PublicStage stage;

  @Column(name = "current_distribution_plan_id")
  UUID currentDistributionPlanId;

  @Column(name = "current_delivery_id")
  UUID currentDeliveryId;

  @Column(name = "deliveries_completed")
  int deliveriesCompleted;

  @Column(name = "last_delivery_at")
  LocalDateTime lastDeliveryAt;

  @Column(name = "last_delivery_verified")
  boolean lastDeliveryVerified;

  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected ZonePublicStatusJpaEntity() {}

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = LocalDateTime.now();
  }
}
