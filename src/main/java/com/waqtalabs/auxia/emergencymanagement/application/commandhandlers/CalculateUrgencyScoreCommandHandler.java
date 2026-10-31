package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.CalculateUrgencyScoreCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.UrgencyScoreCalculated;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.StructuredFieldData;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.UrgencyIndicators;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.services.UrgencyScoringService;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Calculates the urgency score of a zone from its data and its processed field reports, and
 * prioritizes the zone (US-04).
 */
@Service
public class CalculateUrgencyScoreCommandHandler {

  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final UrgencyScoringService urgencyScoringService;
  private final DomainEventPublisher eventPublisher;

  public CalculateUrgencyScoreCommandHandler(
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      UrgencyScoringService urgencyScoringService,
      DomainEventPublisher eventPublisher) {
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.urgencyScoringService = urgencyScoringService;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Zone handle(CalculateUrgencyScoreCommand command) {
    Zone zone =
        zoneRepository
            .findById(command.zoneId())
            .orElseThrow(() -> new ZoneNotFoundException(command.zoneId()));
    List<StructuredFieldData> reports =
        fieldReportRepository.findByZoneId(zone.id()).stream()
            .map(FieldReport::structuredData)
            .flatMap(Optional::stream)
            .toList();
    UrgencyIndicators indicators = zone.urgencyIndicators(reports);
    zone.updateUrgencyScore(urgencyScoringService.calculateScore(indicators));
    Zone saved = zoneRepository.save(zone);
    eventPublisher.publish(new UrgencyScoreCalculated(saved.id()));
    return saved;
  }
}
