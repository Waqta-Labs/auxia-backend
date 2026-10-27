package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;

final class FieldReportJpaMapper {

  private FieldReportJpaMapper() {}

  static FieldReport toDomain(FieldReportJpaEntity entity) {
    return FieldReport.reconstitute(
        new FieldReportId(entity.id),
        new EmergencyId(entity.emergencyId),
        entity.zoneId == null ? null : new ZoneId(entity.zoneId),
        entity.rawText,
        entity.source,
        entity.reportedAt);
  }

  /** Copies the aggregate into a new or managed entity, keeping the columns it does not own. */
  static void copy(FieldReport fieldReport, FieldReportJpaEntity entity) {
    entity.id = fieldReport.id().value();
    entity.emergencyId = fieldReport.emergencyId().value();
    entity.zoneId = fieldReport.zoneId().map(ZoneId::value).orElse(null);
    entity.rawText = fieldReport.rawText();
    entity.source = fieldReport.source();
    entity.reportedAt = fieldReport.reportedAt();
  }
}
