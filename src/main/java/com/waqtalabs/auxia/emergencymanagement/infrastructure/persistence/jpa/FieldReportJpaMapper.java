package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.List;
import java.util.Set;

final class FieldReportJpaMapper {

  private FieldReportJpaMapper() {}

  static FieldReport toDomain(FieldReportJpaEntity entity) {
    return FieldReport.reconstitute(
        new FieldReportId(entity.id),
        new EmergencyId(entity.emergencyId),
        entity.zoneId == null ? null : new ZoneId(entity.zoneId),
        entity.rawText,
        entity.source,
        entity.reportedAt,
        structuredData(entity));
  }

  /** Copies the aggregate into a new or managed entity, keeping the columns it does not own. */
  static void copy(FieldReport fieldReport, FieldReportJpaEntity entity) {
    entity.id = fieldReport.id().value();
    entity.emergencyId = fieldReport.emergencyId().value();
    entity.zoneId = fieldReport.zoneId().map(ZoneId::value).orElse(null);
    entity.rawText = fieldReport.rawText();
    entity.source = fieldReport.source();
    entity.reportedAt = fieldReport.reportedAt();
    entity.extractedIndicators =
        fieldReport.structuredData().map(FieldReportJpaMapper::toJson).orElse(null);
    entity.missingVariables =
        fieldReport.structuredData().map(StructuredFieldData::missingVariables).orElse(null);
  }

  private static StructuredFieldData structuredData(FieldReportJpaEntity entity) {
    ExtractedIndicatorsJson indicators = entity.extractedIndicators;
    if (indicators == null) {
      return null;
    }
    return new StructuredFieldData(
        indicators.damageLevel(),
        indicators.familiesAffected(),
        indicators.peopleAffected(),
        indicators.children(),
        indicators.elderlyPeople(),
        indicators.disabledPeople(),
        indicators.needs() == null ? null : Set.copyOf(indicators.needs()),
        entity.missingVariables);
  }

  private static ExtractedIndicatorsJson toJson(StructuredFieldData data) {
    return new ExtractedIndicatorsJson(
        data.damageLevel(),
        data.familiesAffected(),
        data.peopleAffected(),
        data.children(),
        data.elderlyPeople(),
        data.disabledPeople(),
        List.copyOf(data.needs()));
  }
}
