package com.waqtalabs.auxia.emergencymanagement.application;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Test double of the repository, from the most recent report. */
public class InMemoryFieldReportRepository implements FieldReportRepository {

  private final Map<FieldReportId, FieldReport> reports = new LinkedHashMap<>();

  @Override
  public FieldReport save(FieldReport fieldReport) {
    reports.put(fieldReport.id(), fieldReport);
    return fieldReport;
  }

  @Override
  public Optional<FieldReport> findById(FieldReportId id) {
    return Optional.ofNullable(reports.get(id));
  }

  @Override
  public List<FieldReport> findByZoneId(ZoneId zoneId) {
    return find(report -> report.zoneId().filter(zoneId::equals).isPresent());
  }

  @Override
  public List<FieldReport> findByEmergencyId(EmergencyId emergencyId) {
    return find(report -> report.emergencyId().equals(emergencyId));
  }

  @Override
  public List<FieldReport> findPendingZoneAssignmentByEmergencyId(EmergencyId emergencyId) {
    return find(
        report -> report.emergencyId().equals(emergencyId) && report.isPendingZoneAssignment());
  }

  private List<FieldReport> find(Predicate<FieldReport> filter) {
    return reports.values().stream()
        .filter(filter)
        .sorted(Comparator.comparing(FieldReport::reportedAt).reversed())
        .toList();
  }
}
