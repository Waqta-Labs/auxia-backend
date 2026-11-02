package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.BrigadeStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Row of {@code resource_management.brigades} with its members and assignments. Only {@link
 * BrigadeJpaMapper} uses it.
 */
@Entity
@Table(schema = "resource_management", name = "brigades")
class BrigadeJpaEntity {

  @Id UUID id;

  @Column(name = "organization_id")
  UUID organizationId;

  String name;

  @Column(name = "base_latitude")
  double baseLatitude;

  @Column(name = "base_longitude")
  double baseLongitude;

  @Enumerated(EnumType.STRING)
  BrigadeStatus status;

  @OneToMany(mappedBy = "brigade", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("fullName")
  List<StaffMemberJpaEntity> members = new ArrayList<>();

  @OneToMany(mappedBy = "brigade", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("assignedAt")
  List<BrigadeAssignmentJpaEntity> assignments = new ArrayList<>();

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected BrigadeJpaEntity() {}
}
