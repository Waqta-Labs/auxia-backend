package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterEmergencyCommandHandler {

  private final EmergencyRepository emergencyRepository;

  public RegisterEmergencyCommandHandler(EmergencyRepository emergencyRepository) {
    this.emergencyRepository = emergencyRepository;
  }

  @Transactional
  public Emergency handle(RegisterEmergencyCommand command) {
    Emergency emergency =
        Emergency.register(
            command.name(),
            command.type(),
            command.description(),
            command.location(),
            command.startDate());
    return emergencyRepository.save(emergency);
  }
}
