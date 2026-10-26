package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterZoneCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Zone;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.ZoneRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads the emergency first, so a closed emergency rejects the new zone. */
@Service
public class RegisterZoneCommandHandler {

  private final EmergencyRepository emergencyRepository;
  private final ZoneRepository zoneRepository;

  public RegisterZoneCommandHandler(
      EmergencyRepository emergencyRepository, ZoneRepository zoneRepository) {
    this.emergencyRepository = emergencyRepository;
    this.zoneRepository = zoneRepository;
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
    return zoneRepository.save(zone);
  }
}
