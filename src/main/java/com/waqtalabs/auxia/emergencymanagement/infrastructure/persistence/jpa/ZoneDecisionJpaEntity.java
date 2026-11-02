package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneDecisionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/** Row of {@code emergency_management.zone_decisions}. Rows are only inserted. */
@Entity
@Table(schema = "emergency_management", name = "zone_decisions")
class ZoneDecisionJpaEntity {

  @Id UUID id;

  @Column(name = "zone_id")
  UUID zoneId;

  @Enumerated(EnumType.STRING)
  ZoneDecisionType type;

  @Column(name = "decided_by")
  UUID decidedBy;

  @Column(name = "decided_at")
  LocalDateTime decidedAt;

  String detail;

  @Column(name = "subject_id")
  UUID subjectId;

  @Column(name = "handed_over_to")
  UUID handedOverTo;

  protected ZoneDecisionJpaEntity() {}
}
