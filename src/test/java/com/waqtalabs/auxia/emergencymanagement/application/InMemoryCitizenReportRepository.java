package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.CitizenReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Test double of the repository. */
public class InMemoryCitizenReportRepository implements CitizenReportRepository {

  private final Map<CitizenReportId, CitizenReport> reports = new LinkedHashMap<>();

  @Override
  public CitizenReport save(CitizenReport citizenReport) {
    reports.put(citizenReport.id(), citizenReport);
    return citizenReport;
  }

  @Override
  public Optional<CitizenReport> findById(CitizenReportId id) {
    return Optional.ofNullable(reports.get(id));
  }

  @Override
  public Optional<CitizenReport> findByFieldReportId(FieldReportId fieldReportId) {
    return reports.values().stream()
        .filter(report -> report.fieldReportId().filter(fieldReportId::equals).isPresent())
        .findFirst();
  }

  public int count() {
    return reports.size();
  }
}
