package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignFieldReportZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AssignFieldReportZoneCommandHandler {

  private final FieldReportRepository fieldReportRepository;
  private final ZoneRepository zoneRepository;

  public AssignFieldReportZoneCommandHandler(
      FieldReportRepository fieldReportRepository, ZoneRepository zoneRepository) {
    this.fieldReportRepository = fieldReportRepository;
    this.zoneRepository = zoneRepository;
  }

  @Transactional
  public FieldReport handle(AssignFieldReportZoneCommand command) {
    FieldReport report =
        fieldReportRepository
            .findById(command.fieldReportId())
            .orElseThrow(() -> new FieldReportNotFoundException(command.fieldReportId()));
    Zone zone =
        zoneRepository
            .findById(command.zoneId())
            .orElseThrow(() -> new ZoneNotFoundException(command.zoneId()));
    report.assignToZone(zone);
    return fieldReportRepository.save(report);
  }
}
