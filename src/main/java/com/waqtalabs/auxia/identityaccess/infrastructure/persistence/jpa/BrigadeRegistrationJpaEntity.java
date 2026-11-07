package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/** Row of {@code identity_access.brigade_registrations}. */
@Entity
@Table(schema = "identity_access", name = "brigade_registrations")
class BrigadeRegistrationJpaEntity {

  @Id UUID id;

  @Column(name = "full_name")
  String fullName;

  String phone;

  @Column(name = "organization_name")
  String organizationName;

  @Column(name = "brigade_code")
  String brigadeCode;

  @Column(name = "brigade_id")
  UUID brigadeId;

  @Column(name = "organization_id")
  UUID organizationId;

  @Column(name = "device_secret_hash")
  String deviceSecretHash;

  @Column(name = "device_secret_algorithm")
  String deviceSecretAlgorithm;

  @Column(name = "id_card_url")
  String idCardUrl;

  @Column(name = "id_card_content_type")
  String idCardContentType;

  @Enumerated(EnumType.STRING)
  RegistrationStatus status;

  @Column(name = "rejection_reason")
  String rejectionReason;

  @Column(name = "submitted_at")
  LocalDateTime submittedAt;

  @Column(name = "decided_by")
  UUID decidedBy;

  @Column(name = "decided_at")
  LocalDateTime decidedAt;

  @Column(name = "user_id")
  UUID userId;

  protected BrigadeRegistrationJpaEntity() {}
}
