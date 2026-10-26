package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.CloseEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.exceptions.EmergencyNotFoundException;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseEmergencyCommandHandler {

  private final EmergencyRepository emergencyRepository;

  public CloseEmergencyCommandHandler(EmergencyRepository emergencyRepository) {
    this.emergencyRepository = emergencyRepository;
  }

  @Transactional
  public Emergency handle(CloseEmergencyCommand command) {
    Emergency emergency =
        emergencyRepository
            .findById(command.emergencyId())
            .orElseThrow(() -> new EmergencyNotFoundException(command.emergencyId()));
    emergency.close();
    return emergencyRepository.save(emergency);
  }
}
