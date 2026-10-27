package com.waqtalabs.auxia.resourcemanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.resourcemanagement.domain.model.valueobjects.StaffRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

/** Row of {@code resource_management.staff_members}. */
@Entity
@Table(schema = "resource_management", name = "staff_members")
class StaffMemberJpaEntity {

  @Id UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "brigade_id")
  BrigadeJpaEntity brigade;

  @Column(name = "full_name")
  String fullName;

  @Enumerated(EnumType.STRING)
  StaffRole role;

  String phone;

  protected StaffMemberJpaEntity() {}
}
