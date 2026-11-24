package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AttachmentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/** Row of {@code emergency_management.citizen_report_attachments}. */
@Entity
@Table(schema = "emergency_management", name = "citizen_report_attachments")
class SosAttachmentJpaEntity {

  @Id UUID id;

  @Column(name = "citizen_report_id")
  UUID citizenReportId;

  @Enumerated(EnumType.STRING)
  AttachmentType type;

  @Column(name = "content_type")
  String contentType;

  @Column(name = "size_bytes")
  long size;

  @Column(name = "storage_url")
  String storageUrl;

  @Column(name = "received_at")
  LocalDateTime receivedAt;

  protected SosAttachmentJpaEntity() {}
}
