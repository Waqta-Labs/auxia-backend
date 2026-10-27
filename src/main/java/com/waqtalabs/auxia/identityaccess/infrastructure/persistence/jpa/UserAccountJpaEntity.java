package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
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

/** Row of {@code identity_access.user_accounts}. Only {@link UserAccountJpaMapper} uses it. */
@Entity
@Table(schema = "identity_access", name = "user_accounts")
class UserAccountJpaEntity {

  @Id UUID id;

  @Column(name = "organization_id")
  UUID organizationId;

  String email;

  @Column(name = "password_hash")
  String passwordHash;

  @Column(name = "hash_algorithm")
  String hashAlgorithm;

  @Column(name = "first_name")
  String firstName;

  @Column(name = "last_name")
  String lastName;

  @Enumerated(EnumType.STRING)
  Role role;

  @Enumerated(EnumType.STRING)
  AccountStatus status;

  @Column(name = "failed_login_attempts")
  int failedLoginAttempts;

  @Column(name = "last_login_at")
  LocalDateTime lastLoginAt;

  @CreationTimestamp
  @Column(name = "created_at", updatable = false)
  LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at")
  LocalDateTime updatedAt;

  protected UserAccountJpaEntity() {}
}
