package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.ChangeBrigadeAvailabilityCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChangeBrigadeAvailabilityCommandHandler {

  private final BrigadeRepository brigadeRepository;

  public ChangeBrigadeAvailabilityCommandHandler(BrigadeRepository brigadeRepository) {
    this.brigadeRepository = brigadeRepository;
  }

  @Transactional
  public Brigade handle(ChangeBrigadeAvailabilityCommand command) {
    Brigade brigade =
        brigadeRepository
            .findById(command.brigadeId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new BrigadeNotFoundException(command.brigadeId()));
    brigade.changeAvailability(command.status());
    return brigadeRepository.save(brigade);
  }
}
