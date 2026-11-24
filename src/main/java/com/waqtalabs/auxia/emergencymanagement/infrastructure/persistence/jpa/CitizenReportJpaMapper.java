package com.waqtalabs.auxia.emergencymanagement.infrastructure.persistence.jpa;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.Geolocation;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.VulnerablePeople;
import java.util.List;
import java.util.Optional;

final class CitizenReportJpaMapper {

  private CitizenReportJpaMapper() {}

  static CitizenReport toDomain(CitizenReportJpaEntity entity) {
    return CitizenReport.reconstitute(
        new CitizenReportId(entity.id),
        entity.category,
        entity.text,
        entity.reportedAt,
        entity.receivedAt,
        entity.latitude == null ? null : new Geolocation(entity.latitude, entity.longitude),
        new VulnerablePeople(entity.children, entity.elderlyPeople, entity.injuredPeople),
        entity.accessConditions,
        entity.relatedReportId == null ? null : new CitizenReportId(entity.relatedReportId),
        entity.fieldReportId == null ? null : new FieldReportId(entity.fieldReportId));
  }

  static CitizenReportJpaEntity toEntity(CitizenReport report) {
    var entity = new CitizenReportJpaEntity();
    entity.id = report.id().value();
    entity.category = report.category();
    entity.text = report.text();
    entity.reportedAt = report.reportedAt();
    entity.receivedAt = report.receivedAt();
    Optional<Geolocation> location = report.location();
    entity.latitude = location.map(Geolocation::latitude).orElse(null);
    entity.longitude = location.map(Geolocation::longitude).orElse(null);
    entity.children = report.people().children();
    entity.elderlyPeople = report.people().elderly();
    entity.injuredPeople = report.people().injured();
    entity.accessConditions = List.copyOf(report.accessConditions());
    entity.relatedReportId = report.relatedReportId().map(CitizenReportId::value).orElse(null);
    entity.fieldReportId = report.fieldReportId().map(FieldReportId::value).orElse(null);
    return entity;
  }
}
