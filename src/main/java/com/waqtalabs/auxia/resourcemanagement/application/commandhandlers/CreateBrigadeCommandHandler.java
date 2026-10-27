package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.CreateBrigadeCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.DuplicateBrigadeException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Brigade names are unique inside an organization, ignoring case. */
@Service
public class CreateBrigadeCommandHandler {

  private final BrigadeRepository brigadeRepository;

  public CreateBrigadeCommandHandler(BrigadeRepository brigadeRepository) {
    this.brigadeRepository = brigadeRepository;
  }

  @Transactional
  public Brigade handle(CreateBrigadeCommand command) {
    Brigade brigade =
        Brigade.create(command.organizationId(), command.name(), command.baseLocation());
    boolean nameTaken =
        brigadeRepository.findByOrganizationId(command.organizationId()).stream()
            .anyMatch(existing -> existing.hasName(brigade.name()));
    if (nameTaken) {
      throw new DuplicateBrigadeException(brigade.name());
    }
    return brigadeRepository.save(brigade);
  }
}
