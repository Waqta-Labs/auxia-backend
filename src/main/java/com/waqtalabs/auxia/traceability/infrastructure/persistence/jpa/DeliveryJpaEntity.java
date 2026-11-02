package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.AnchorStatus;
import com.waqtalabs.auxia.traceability.domain.model.valueobjects.DeliveryStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "traceability", name = "deliveries")
class DeliveryJpaEntity {

  @Id UUID id;

  @Column(name = "distribution_plan_id")
  UUID distributionPlanId;

  @Column(name = "zone_id")
  UUID zoneId;

  @Column(name = "brigade_id")
  UUID brigadeId;

  @Column(name = "organization_id")
  UUID organizationId;

  @Enumerated(EnumType.STRING)
  DeliveryStatus status;

  @Column(name = "registered_by")
  UUID registeredBy;

  @Column(name = "delivered_at")
  LocalDateTime deliveredAt;

  @Column(name = "received_at")
  LocalDateTime receivedAt;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "record_hash", length = 64)
  String recordHash;

  @Enumerated(EnumType.STRING)
  @Column(name = "anchor_status")
  AnchorStatus anchorStatus;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "transaction_hash", length = 66)
  String transactionHash;

  @Column(name = "anchored_at")
  LocalDateTime anchoredAt;

  @Column(name = "anchor_attempts")
  int anchorAttempts;

  /** Filled by the database; it orders the deliveries of a zone. */
  @Column(name = "created_at", insertable = false, updatable = false)
  LocalDateTime createdAt;

  @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
  List<DeliveryItemJpaEntity> items = new ArrayList<>();

  @OneToMany(mappedBy = "delivery", cascade = CascadeType.ALL, orphanRemoval = true)
  List<EvidenceJpaEntity> evidences = new ArrayList<>();

  protected DeliveryJpaEntity() {}
}
