package com.waqtalabs.auxia.emergencymanagement.domain.repositories;

import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.FieldReportId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import java.util.List;
import java.util.Optional;

/** Every list is ordered from the most recent report to the oldest. */
public interface FieldReportRepository {

  FieldReport save(FieldReport fieldReport);

  Optional<FieldReport> findById(FieldReportId id);

  List<FieldReport> findByZoneId(ZoneId zoneId);

  List<FieldReport> findByEmergencyId(EmergencyId emergencyId);

  /** Reports of the emergency that still have no zone. */
  List<FieldReport> findPendingZoneAssignmentByEmergencyId(EmergencyId emergencyId);
}
