package com.waqtalabs.auxia.resourcemanagement.application.commandhandlers;

import com.waqtalabs.auxia.resourcemanagement.application.commands.AddStaffMemberCommand;
import com.waqtalabs.auxia.resourcemanagement.application.ports.BrigadistAccounts;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.AccountInAnotherBrigadeException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadeNotFoundException;
import com.waqtalabs.auxia.resourcemanagement.domain.exceptions.BrigadistAccountRequiredException;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.Brigade;
import com.waqtalabs.auxia.resourcemanagement.domain.model.aggregates.StaffMember;
import com.waqtalabs.auxia.resourcemanagement.domain.repositories.BrigadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Adds a member to a brigade. A member with an account is an active FIELD_BRIGADE of the same
 * organization that is not in another brigade, so the mobile app shows that account the missions
 * of this brigade (A_07).
 */
@Service
public class AddStaffMemberCommandHandler {

  private final BrigadeRepository brigadeRepository;
  private final BrigadistAccounts brigadistAccounts;

  public AddStaffMemberCommandHandler(
      BrigadeRepository brigadeRepository, BrigadistAccounts brigadistAccounts) {
    this.brigadeRepository = brigadeRepository;
    this.brigadistAccounts = brigadistAccounts;
  }

  /**
   * @throws BrigadeNotFoundException if the brigade is not of the organization
   * @throws BrigadistAccountRequiredException if the account is not an active brigadist of it
   * @throws AccountInAnotherBrigadeException if the account is already a member of a brigade
   */
  @Transactional
  public Brigade handle(AddStaffMemberCommand command) {
    Brigade brigade =
        brigadeRepository
            .findById(command.brigadeId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(() -> new BrigadeNotFoundException(command.brigadeId()));
    if (command.userId() != null) {
      if (!brigadistAccounts.isActiveBrigadist(command.userId(), command.organizationId())) {
        throw new BrigadistAccountRequiredException(command.userId());
      }
      if (brigadeRepository.findByMemberAccount(command.userId()).isPresent()) {
        throw new AccountInAnotherBrigadeException(command.userId());
      }
    }
    brigade.addMember(
        StaffMember.register(
            command.fullName(), command.role(), command.phone(), command.userId()));
    return brigadeRepository.save(brigade);
  }
}
