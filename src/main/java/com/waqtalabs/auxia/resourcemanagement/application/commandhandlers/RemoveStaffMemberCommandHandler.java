package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.RemoveStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RemoveStaffMemberCommandHandler {

  private final BrigadeRepository brigadeRepository;

  public RemoveStaffMemberCommandHandler(BrigadeRepository brigadeRepository) {
    this.brigadeRepository = brigadeRepository;
  }

  @Transactional
  public Brigade handle(RemoveStaffMemberCommand command) {
    Brigade brigade =
        brigadeRepository
            .findById(command.brigadeId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new BrigadeNotFoundException(command.brigadeId()));
    brigade.removeMember(command.memberId());
    return brigadeRepository.save(brigade);
  }
}
