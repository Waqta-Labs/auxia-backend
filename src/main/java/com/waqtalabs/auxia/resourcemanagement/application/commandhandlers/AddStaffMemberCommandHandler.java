package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AddStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AddStaffMemberCommandHandler {

  private final BrigadeRepository brigadeRepository;

  public AddStaffMemberCommandHandler(BrigadeRepository brigadeRepository) {
    this.brigadeRepository = brigadeRepository;
  }

  @Transactional
  public Brigade handle(AddStaffMemberCommand command) {
    Brigade brigade =
        brigadeRepository
            .findById(command.brigadeId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new BrigadeNotFoundException(command.brigadeId()));
    brigade.addMember(StaffMember.register(command.fullName(), command.role(), command.phone()));
    return brigadeRepository.save(brigade);
  }
}
