package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DamageLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.PriorityLevel;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/**
 * Row of {@code emergency_management.zones}. Only {@link ZoneJpaMapper} uses it.
 *
 * <p>The {@code urgency_*} columns hold the UrgencyScore value object: all of them are {@code
 * null} until the score is calculated. The {@code manual_priority_*} columns hold the
 * ManualPriority value object in the same way.
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

  @Column(name = "last_aid_received_at")
  LocalDateTime lastAidReceivedAt;

  @Column(name = "manual_priority_by")
  UUID manualPriorityBy;

  @Column(name = "manual_priority_at")
  LocalDateTime manualPriorityAt;

  @Column(name = "manual_priority_reason")
  String manualPriorityReason;

  @Column(name = "responsible_authority_id")
  UUID responsibleAuthorityId;

  @Column(name = "urgency_score", precision = 5, scale = 2)
  BigDecimal urgencyScore;

  @Enumerated(EnumType.STRING)
  @Column(name = "urgency_level")
  PriorityLevel urgencyLevel;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "urgency_top_features")
  List<ScoreContributionJson> urgencyTopFeatures;

  @Column(name = "urgency_model_version")
  String urgencyModelVersion;

  @Column(name = "urgency_calculated_at")
  LocalDateTime urgencyCalculatedAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected ZoneJpaEntity() {}
}
