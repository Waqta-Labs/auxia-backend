package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterFieldReportCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.ZoneNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.EmergencyId;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ZoneId;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.services.ZoneIdentificationService;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers a field report and associates it with its zone (US-01).
 *
 * <p>The zone indicated by the authority is used as is. Without one, the handler tries to
 * identify it from the text among the zones of the emergency; if it cannot, the report stays
 * pending zone assignment. It publishes FieldReportRegistered, which starts the structuring of
 * the report.
 */
@Service
public class RegisterFieldReportCommandHandler {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final ZoneIdentificationService zoneIdentification = new ZoneIdentificationService();
  private final Clock clock;
  private final DomainEventPublisher eventPublisher;

  public RegisterFieldReportCommandHandler(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      Clock clock,
      DomainEventPublisher eventPublisher) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.clock = clock;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public FieldReport handle(RegisterFieldReportCommand command) {
    Optional<Zone> indicatedZone = Optional.ofNullable(command.zoneId()).map(this::loadZone);
    EmergencyId emergencyId =
        command.emergencyId() != null
            ? loadEmergencyId(command.emergencyId())
            : indicatedZone.orElseThrow().emergencyId();

    FieldReport report =
        FieldReport.register(
            emergencyId, command.rawText(), command.source(), LocalDateTime.now(clock));
    indicatedZone
        .or(
            () ->
                zoneIdentification.identifyZone(
                    command.rawText(), zoneRepository.findByEmergencyId(emergencyId)))
        .ifPresent(report::assignToZone);
    FieldReport saved = fieldReportRepository.save(report);
    eventPublisher.publish(new FieldReportRegistered(saved.id()));
    return saved;
  }

  private Zone loadZone(ZoneId id) {
    return zoneRepository.findById(id).orElseThrow(() -> new ZoneNotFoundException(id));
  }

  private EmergencyId loadEmergencyId(EmergencyId id) {
    return emergencyRepository
        .findById(id)
        .orElseThrow(() -> new EmergencyNotFoundException(id))
        .id();
  }
}
