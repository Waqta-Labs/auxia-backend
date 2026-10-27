package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Row of {@code emergency_management.field_reports}. Only {@link FieldReportJpaMapper} uses it.
 *
 * <p>The structured data of the NLP is not mapped yet: Hibernate leaves those columns untouched
 * until the AI Service integration.
 */
@Entity
@Table(schema = "emergency_management", name = "field_reports")
class FieldReportJpaEntity {

  @Id UUID id;

  @Column(name = "emergency_id")
  UUID emergencyId;

  @Column(name = "zone_id")
  UUID zoneId;

  @Column(name = "raw_text")
  String rawText;

  @Enumerated(EnumType.STRING)
  ReportSource source;

  @Column(name = "reported_at")
  LocalDateTime reportedAt;

  protected FieldReportJpaEntity() {}
}
