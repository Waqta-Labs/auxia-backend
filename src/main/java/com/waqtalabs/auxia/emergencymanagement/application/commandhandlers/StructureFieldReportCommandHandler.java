package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.StructureFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.FieldReportStructuringService;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportProcessed;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportAlreadyProcessedException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.FieldReportNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Extracts the indicators of a field report with NLP and stores them (US-02). It publishes
 * FieldReportProcessed so the score of the zone is recalculated.
 */
@Service
public class StructureFieldReportCommandHandler {

  private final FieldReportRepository fieldReportRepository;
  private final FieldReportStructuringService structuringService;
  private final DomainEventPublisher eventPublisher;

  public StructureFieldReportCommandHandler(
      FieldReportRepository fieldReportRepository,
      FieldReportStructuringService structuringService,
      DomainEventPublisher eventPublisher) {
    this.fieldReportRepository = fieldReportRepository;
    this.structuringService = structuringService;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public FieldReport handle(StructureFieldReportCommand command) {
    FieldReport report =
        fieldReportRepository
            .findById(command.fieldReportId())
            .orElseThrow(() -> new FieldReportNotFoundException(command.fieldReportId()));
    // Checked before calling the AI Service, so a processed report does not cost a call.
    if (report.isProcessed()) {
      throw new FieldReportAlreadyProcessedException(report.id());
    }
    report.markProcessed(structuringService.structure(report.rawText()));
    FieldReport saved = fieldReportRepository.save(report);
    eventPublisher.publish(new FieldReportProcessed(saved.id(), saved.zoneId().orElse(null)));
    return saved;
  }
}
