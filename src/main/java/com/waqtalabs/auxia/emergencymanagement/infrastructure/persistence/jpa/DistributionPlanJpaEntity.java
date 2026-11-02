package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Coverage;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.DistributionPlanStatus;
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

@Entity
@Table(schema = "emergency_management", name = "distribution_plans")
class DistributionPlanJpaEntity {

  @Id UUID id;

  @Column(name = "emergency_id")
  UUID emergencyId;

  @Column(name = "zone_id")
  UUID zoneId;

  @Column(name = "organization_id")
  UUID organizationId;

  @Enumerated(EnumType.STRING)
  DistributionPlanStatus status;

  @Enumerated(EnumType.STRING)
  Coverage coverage;

  String justification;

  @Column(name = "model_version")
  String modelVersion;

  /** The authority who approved or rejected the plan, as the report names the column. */
  @Column(name = "approved_by")
  UUID decidedBy;

  @Column(name = "approved_at")
  LocalDateTime decidedAt;

  @Column(name = "rejected_reason")
  String rejectedReason;

  @Column(name = "modified_by")
  UUID modifiedBy;

  @Column(name = "modified_at")
  LocalDateTime modifiedAt;

  @Column(name = "modification_reason")
  String modificationReason;

  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
  List<DistributionItemJpaEntity> items = new ArrayList<>();

  protected DistributionPlanJpaEntity() {}
}
