package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Row of {@code emergency_management.field_reports}. Only {@link FieldReportJpaMapper} uses it.
 *
 * <p>The two JSONB columns hold the structured data of the NLP. Both are {@code null} while the
 * report is not processed.
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

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "extracted_indicators")
  ExtractedIndicatorsJson extractedIndicators;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "missing_variables")
  List<String> missingVariables;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "corrected_indicators")
  ExtractedIndicatorsJson correctedIndicators;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "corrected_missing_variables")
  List<String> correctedMissingVariables;

  @Column(name = "corrected_by")
  UUID correctedBy;

  @Column(name = "corrected_at")
  LocalDateTime correctedAt;

  @Column(name = "correction_reason")
  String correctionReason;

  protected FieldReportJpaEntity() {}
}
