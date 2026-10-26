package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Row of {@code emergency_management.zones}. Only {@link ZoneJpaMapper} uses it.
 *
 * <p>The urgency score and the last aid received are not mapped yet: Hibernate leaves those
 * columns untouched until the phases that use them.
 */
@Entity
@Table(schema = "emergency_management", name = "zones")
class ZoneJpaEntity {

  @Id UUID id;

  @Column(name = "emergency_id")
  UUID emergencyId;

  String name;

  Double latitude;

  Double longitude;

  Integer population;

  @Column(name = "families_affected")
  int familiesAffected;

  @Column(name = "people_affected")
  int peopleAffected;

  int children;

  @Column(name = "elderly_people")
  int elderlyPeople;

  @Column(name = "disabled_people")
  int disabledPeople;

  @Enumerated(EnumType.STRING)
  @Column(name = "damage_level")
  DamageLevel damageLevel;

  @Enumerated(EnumType.STRING)
  ZoneStatus status;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected ZoneJpaEntity() {}
}
