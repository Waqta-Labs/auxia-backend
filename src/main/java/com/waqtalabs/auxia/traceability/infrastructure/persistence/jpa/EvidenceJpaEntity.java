package com.waqtalabs.auxia.traceability.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.traceability.domain.model.valueobjects.EvidenceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(schema = "traceability", name = "evidences")
class EvidenceJpaEntity {

  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "delivery_id")
  DeliveryJpaEntity delivery;

  @Enumerated(EnumType.STRING)
  EvidenceType type;

  @Column(name = "storage_url")
  String storageUrl;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "content_hash", length = 64)
  String contentHash;

  @Column(name = "captured_at")
  LocalDateTime capturedAt;

  Double latitude;

  Double longitude;

  protected EvidenceJpaEntity() {}
}
