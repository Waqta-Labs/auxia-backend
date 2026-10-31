package com.waqtalabs.auxia.citizentransparency.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(schema = "citizen_transparency", name = "emergency_public_views")
class EmergencyPublicViewJpaEntity {

  @Id
  @Column(name = "emergency_id")
  UUID emergencyId;

  String name;

  String type;

  String department;

  String province;

  String district;

  @Column(name = "start_date")
  LocalDate startDate;

  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected EmergencyPublicViewJpaEntity() {}

  @PrePersist
  @PreUpdate
  void touch() {
    updatedAt = LocalDateTime.now();
  }
}
