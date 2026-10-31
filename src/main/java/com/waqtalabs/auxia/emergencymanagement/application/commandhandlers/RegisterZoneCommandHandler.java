package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.ZoneRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads the emergency first, so a closed emergency rejects the new zone. The authority who
 * registers it becomes its responsible. It publishes ZoneRegistered, which calculates the first
 * urgency score of the zone and opens its history.
 */
@Service
public class RegisterZoneCommandHandler {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;
  private final DomainEventPublisher eventPublisher;

  public RegisterZoneCommandHandler(
      EmergencyRepository emergencyRepository,
      ZoneRepository zoneRepository,
      DomainEventPublisher eventPublisher) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
    this.eventPublisher = eventPublisher;
  }

  @Transactional
  public Zone handle(RegisterZoneCommand command) {
    Emergency emergency =
        emergencyRepository
            .findById(command.emergencyId())
            .orElseThrow(() -> new EmergencyNotFoundException(command.emergencyId()));
    Zone zone =
        emergency.registerZone(
            command.name(),
            command.geolocation(),
            command.populationImpact(),
            command.damageLevel());
    zone.assignResponsible(command.registeredBy());
    Zone saved = zoneRepository.save(zone);
    eventPublisher.publish(
        new ZoneRegistered(saved.id(), saved.emergencyId(), saved.name(), command.registeredBy()));
    return saved;
  }
}
