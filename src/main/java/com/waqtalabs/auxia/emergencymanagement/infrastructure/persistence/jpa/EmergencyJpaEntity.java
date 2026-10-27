package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyStatus;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** Row of {@code emergency_management.emergencies}. Only {@link EmergencyJpaMapper} uses it. */
@Entity
@Table(schema = "emergency_management", name = "emergencies")
class EmergencyJpaEntity {

  @Id UUID id;

  String name;

  @Enumerated(EnumType.STRING)
  EmergencyType type;

  String description;

  String department;

  String province;

  String district;

  @Enumerated(EnumType.STRING)
  EmergencyStatus status;

  @Column(name = "start_date")
  LocalDate startDate;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected EmergencyJpaEntity() {}
}
