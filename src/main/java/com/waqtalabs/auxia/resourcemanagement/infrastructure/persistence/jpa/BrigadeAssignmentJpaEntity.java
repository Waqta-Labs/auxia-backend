package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.AssignmentStatus;
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

/** Row of {@code resource_management.brigade_assignments}. */
@Entity
@Table(schema = "resource_management", name = "brigade_assignments")
class BrigadeAssignmentJpaEntity {

  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brigade_id")
  BrigadeJpaEntity brigade;

  @Column(name = "distribution_plan_id")
  UUID distributionPlanId;

  @Column(name = "zone_id")
  UUID zoneId;

  @Enumerated(EnumType.STRING)
  AssignmentStatus status;

  @Column(name = "assigned_at")
  LocalDateTime assignedAt;

  @Column(name = "completed_at")
  LocalDateTime completedAt;

  protected BrigadeAssignmentJpaEntity() {}
}
