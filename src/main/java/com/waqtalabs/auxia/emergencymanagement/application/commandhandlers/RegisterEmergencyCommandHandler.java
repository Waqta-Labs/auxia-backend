package com.waqtalabs.auxia.emergencymanagement.application.commandhandlers;

import com.waqtalabs.auxia.emergencymanagement.application.commands.RegisterEmergencyCommand;
import com.waqtalabs.auxia.emergencymanagement.domain.events.EmergencyRegistered;
import com.waqtalabs.auxia.emergencymanagement.domain.model.aggregates.Emergency;
import com.waqtalabs.auxia.emergencymanagement.domain.repositories.EmergencyRepository;
import com.waqtalabs.auxia.shared.application.events.DomainEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterEmergencyCommandHandler {

  private final EmergencyRepository emergencyRepository;
  private final DomainEventPublisher eventPublisher;

  public RegisterEmergencyCommandHandler(
      EmergencyRepository emergencyRepository, DomainEventPublisher eventPublisher) {
    this.emergencyRepository = emergencyRepository;
    this.eventPublisher = eventPublisher;
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
    Emergency saved = emergencyRepository.save(emergency);
    eventPublisher.publish(
        new EmergencyRegistered(
            saved.id(), saved.name(), saved.type(), saved.location(), saved.startDate()));
    return saved;
  }
}
