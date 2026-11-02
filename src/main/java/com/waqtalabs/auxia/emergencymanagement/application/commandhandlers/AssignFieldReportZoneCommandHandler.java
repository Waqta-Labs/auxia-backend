package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.AssignFieldReportZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportAssignedToZone;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** It publishes FieldReportAssignedToZone, so the score of the zone includes the report. */
@Service
public class AssignFieldReportZoneCommandHandler {

  private final FieldReportRepository fieldReportRepository;
  private final ZoneRepository zoneRepository;
  private final DomainEventPublisher eventPublisher;

  public AssignFieldReportZoneCommandHandler(
      FieldReportRepository fieldReportRepository,
      ZoneRepository zoneRepository,
      DomainEventPublisher eventPublisher) {
    this.fieldReportRepository = fieldReportRepository;
    this.zoneRepository = zoneRepository;
    this.eventPublisher = eventPublisher;
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
    FieldReport saved = fieldReportRepository.save(report);
    eventPublisher.publish(new FieldReportAssignedToZone(saved.id(), zone.id()));
    return saved;
  }
}
