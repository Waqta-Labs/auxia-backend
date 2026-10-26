package com.waqtalabs.auxia.emergencymanagement.application.queryservices;

import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportsByEmergencyIdQuery;
import com.waqtalabs.auxia.emergencymanagement.application.queries.GetFieldReportsByZoneIdQuery;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Every list goes from the most recent report to the oldest. */
@Service
@Transactional(readOnly = true)
public class FieldReportQueryService {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;

  public FieldReportQueryService(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
  }

  public List<FieldReport> handle(GetFieldReportsByZoneIdQuery query) {
    if (zoneRepository.findById(query.zoneId()).isEmpty()) {
      throw new ZoneNotFoundException(query.zoneId());
    }
    return fieldReportRepository.findByZoneId(query.zoneId());
  }

  public List<FieldReport> handle(GetFieldReportsByEmergencyIdQuery query) {
    if (emergencyRepository.findById(query.emergencyId()).isEmpty()) {
      throw new EmergencyNotFoundException(query.emergencyId());
    }
    return query.pendingZoneAssignmentOnly()
        ? fieldReportRepository.findPendingZoneAssignmentByEmergencyId(query.emergencyId())
        : fieldReportRepository.findByEmergencyId(query.emergencyId());
  }
}
