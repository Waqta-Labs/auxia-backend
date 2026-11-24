package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AccessCondition;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.SosCategory;
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
 * Row of {@code emergency_management.citizen_reports}. Only {@link CitizenReportJpaMapper} uses
 * it.
 */
@Entity
@Table(schema = "emergency_management", name = "citizen_reports")
class CitizenReportJpaEntity {

  @Id UUID id;

  @Enumerated(EnumType.STRING)
  SosCategory category;

  String text;

  @Column(name = "reported_at")
  LocalDateTime reportedAt;

  @Column(name = "received_at")
  LocalDateTime receivedAt;

  Double latitude;

  Double longitude;

  int children;

  @Column(name = "elderly_people")
  int elderlyPeople;

  @Column(name = "injured_people")
  int injuredPeople;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "access_conditions")
  List<AccessCondition> accessConditions;

  @Column(name = "related_report_id")
  UUID relatedReportId;

  @Column(name = "field_report_id")
  UUID fieldReportId;

  protected CitizenReportJpaEntity() {}
}
