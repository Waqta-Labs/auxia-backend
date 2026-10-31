package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.AuthorityId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportCorrection;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.List;
import java.util.Optional;
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
        structuredData(entity.extractedIndicators, entity.missingVariables),
        entity.correctedIndicators == null
            ? null
            : new ReportCorrection(
                structuredData(entity.correctedIndicators, entity.correctedMissingVariables),
                new AuthorityId(entity.correctedBy),
                entity.correctedAt,
                entity.correctionReason));
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
        fieldReport.originalData().map(FieldReportJpaMapper::toJson).orElse(null);
    entity.missingVariables =
        fieldReport.originalData().map(StructuredFieldData::missingVariables).orElse(null);
    Optional<ReportCorrection> correction = fieldReport.correction();
    entity.correctedIndicators =
        correction
            .map(ReportCorrection::correctedData)
            .map(FieldReportJpaMapper::toJson)
            .orElse(null);
    entity.correctedMissingVariables =
        correction.map(found -> found.correctedData().missingVariables()).orElse(null);
    entity.correctedBy = correction.map(found -> found.correctedBy().value()).orElse(null);
    entity.correctedAt = correction.map(ReportCorrection::correctedAt).orElse(null);
    entity.correctionReason = correction.map(ReportCorrection::reason).orElse(null);
  }

  private static StructuredFieldData structuredData(
      ExtractedIndicatorsJson indicators, List<String> missingVariables) {
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
        missingVariables);
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
