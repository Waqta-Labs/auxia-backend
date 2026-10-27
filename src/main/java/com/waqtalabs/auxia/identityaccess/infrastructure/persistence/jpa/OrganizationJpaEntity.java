package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

/** Row of {@code identity_access.organizations}. Only {@link OrganizationJpaMapper} uses it. */
@Entity
@Table(schema = "identity_access", name = "organizations")
class OrganizationJpaEntity {

  @Id UUID id;

  String name;

  @Enumerated(EnumType.STRING)
  OrganizationType type;

  @JdbcTypeCode(SqlTypes.CHAR)
  @Column(name = "tax_id", length = 11)
  String taxId;

  @Enumerated(EnumType.STRING)
  OrganizationStatus status;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected OrganizationJpaEntity() {}
}
