package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterCitizenReportCommand;
import com.waqtalabs.auxia.emergencymanagement.application.ports.SosRateLimiter;
import com.waqtalabs.auxia.emergencymanagement.domain.events.FieldReportRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.CitizenReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.FieldReport;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.model.valueobjects.ReportSource;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.CitizenReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.FieldReportRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.services.SosPlacementService;
import com.waqtalabs.auxia.emergencymanagement.domain.services.SosPlacementService.Placement;
import com.waqtalabs.auxia.emergencymanagement.domain.services.ZoneIdentificationService;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import com.waqtalabs.auxia.shared.application.exceptions.RateLimitExceededException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers the SOS of a citizen.
 *
 * <p>The phone sends again every SOS it did not see confirmed, so an SOS that is already stored
 * is returned as it is, without counting it for the limit of the sender. A new one is placed in
 * an active emergency, by its location, and registered as a field report of source CITIZEN, pending
 * review like any other: its NLP structuring starts with FieldReportRegistered. An SOS that adds
 * detail to an earlier one goes to the same emergency and zone as the earlier one. An SOS that
 * cannot be placed is kept anyway, so it is never lost.
 */
@Service
public class RegisterCitizenReportCommandHandler {

  private final CitizenReportRepository citizenReportRepository;
  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final FieldReportRepository fieldReportRepository;
  private final SosRateLimiter rateLimiter;
  private final Clock clock;
  private final DomainEventPublisher eventPublisher;
  private final SosPlacementService placementService = new SosPlacementService();
  private final ZoneIdentificationService zoneIdentification = new ZoneIdentificationService();

  public RegisterCitizenReportCommandHandler(
      CitizenReportRepository citizenReportRepository,
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      FieldReportRepository fieldReportRepository,
      SosRateLimiter rateLimiter,
      Clock clock,
      DomainEventPublisher eventPublisher) {
    this.citizenReportRepository = citizenReportRepository;
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.fieldReportRepository = fieldReportRepository;
    this.rateLimiter = rateLimiter;
    this.clock = clock;
    this.eventPublisher = eventPublisher;
  }

  /**
   * The SOS, and whether this request stored it.
   *
   * @param created {@code false} when the SOS was already stored by an earlier request
   */
  public record Registration(CitizenReport report, boolean created) {}

  /**
   * @throws RateLimitExceededException if the SOS is new and its sender reached the limit
   */
  @Transactional
  public Registration handle(RegisterCitizenReportCommand command) {
    Optional<CitizenReport> stored = citizenReportRepository.findById(command.clientReportId());
    if (stored.isPresent()) {
      return new Registration(stored.get(), false);
    }
    rateLimiter.acquire(command.sender());

    LocalDateTime receivedAt = LocalDateTime.now(clock);
    LocalDateTime reportedAt = LocalDateTime.ofInstant(command.reportedAt(), clock.getZone());
    Optional<FieldReport> fieldReport =
        place(command)
            .map(
                placement ->
                    registerFieldReport(
                        command, placement, CitizenReport.reportedTime(reportedAt, receivedAt)));
    CitizenReport report =
        citizenReportRepository.save(
            CitizenReport.receive(
                command.clientReportId(),
                command.category(),
                command.text(),
                reportedAt,
                receivedAt,
                command.location().orElse(null),
                command.people(),
                command.accessConditions(),
                command.relatedReportId().orElse(null),
                fieldReport.map(FieldReport::id).orElse(null)));
    fieldReport.ifPresent(
        registered -> eventPublisher.publish(new FieldReportRegistered(registered.id())));
    return new Registration(report, true);
  }

  private Optional<Placement> place(RegisterCitizenReportCommand command) {
    Optional<Placement> ofEarlierReport =
        command
            .relatedReportId()
            .flatMap(citizenReportRepository::findById)
            .flatMap(CitizenReport::fieldReportId)
            .flatMap(fieldReportRepository::findById)
            .map(
                earlier ->
                    new Placement(
                        earlier.emergencyId(), earlier.zoneId().flatMap(zoneRepository::findById)));
    if (ofEarlierReport.isPresent()) {
      return ofEarlierReport;
    }
    List<Emergency> active = emergencyRepository.findActive();
    List<Zone> zones =
        active.stream()
            .flatMap(emergency -> zoneRepository.findByEmergencyId(emergency.id()).stream())
            .toList();
    return placementService.place(command.location(), active, zones);
  }

  private FieldReport registerFieldReport(
      RegisterCitizenReportCommand command, Placement placement, LocalDateTime reportedAt) {
    FieldReport report =
        FieldReport.register(
            placement.emergencyId(), command.text(), ReportSource.CITIZEN, reportedAt);
    placement
        .zone()
        .or(
            () ->
                zoneIdentification.identifyZone(
                    command.text(), zoneRepository.findByEmergencyId(placement.emergencyId())))
        .ifPresent(report::assignToZone);
    return fieldReportRepository.save(report);
  }
}
