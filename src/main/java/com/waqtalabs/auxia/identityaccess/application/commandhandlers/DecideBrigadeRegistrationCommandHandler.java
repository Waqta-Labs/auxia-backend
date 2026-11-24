package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.DecideBrigadeRegistrationCommand;
import com.waqtalabs.auxia.identityaccess.application.ports.BrigadeDirectory;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.BrigadeRegistrationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.DuplicateEmailException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.RegistrationAlreadyDecidedException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.BrigadeRegistration;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.repositories.BrigadeRegistrationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * An ORGANIZATION_ADMIN approves or rejects the request of a brigadist of their organization
 * (A_02). Approving it creates the FIELD_BRIGADE account, whose credential is the secret of the
 * phone, and adds the brigadist to the brigade of the code, linked to that account (A_07).
 */
@Service
public class DecideBrigadeRegistrationCommandHandler {

  private final BrigadeRegistrationRepository registrationRepository;
  private final UserAccountRepository userAccountRepository;
  private final BrigadeDirectory brigadeDirectory;
  private final Clock clock;

  public DecideBrigadeRegistrationCommandHandler(
      BrigadeRegistrationRepository registrationRepository,
      UserAccountRepository userAccountRepository,
      BrigadeDirectory brigadeDirectory,
      Clock clock) {
    this.registrationRepository = registrationRepository;
    this.userAccountRepository = userAccountRepository;
    this.brigadeDirectory = brigadeDirectory;
    this.clock = clock;
  }

  /**
   * @throws BrigadeRegistrationNotFoundException if it is not a request of the organization
   * @throws RegistrationAlreadyDecidedException if it was already decided
   */
  @Transactional
  public BrigadeRegistration handle(DecideBrigadeRegistrationCommand command) {
    BrigadeRegistration registration =
        registrationRepository
            .findById(command.registrationId())
            .filter(found -> found.belongsTo(command.organizationId()))
            .orElseThrow(
                () -> new BrigadeRegistrationNotFoundException(command.registrationId()));
    LocalDateTime now = LocalDateTime.now(clock);
    if (command.rejectionReason() != null) {
      registration.reject(command.rejectionReason(), command.decidedBy(), now);
      return registrationRepository.save(registration);
    }
    if (userAccountRepository.existsByEmail(registration.accountEmail())) {
      throw new DuplicateEmailException(registration.accountEmail());
    }
    UserAccount account =
        userAccountRepository.save(
            UserAccount.register(
                registration.organizationId(),
                registration.accountEmail(),
                registration.accountName(),
                Role.FIELD_BRIGADE,
                registration.deviceSecret()));
    registration.approve(account.id(), command.decidedBy(), now);
    brigadeDirectory.addBrigadist(
        registration.brigadeId(),
        registration.organizationId(),
        registration.fullName(),
        registration.phone(),
        account.id());
    return registrationRepository.save(registration);
  }
}
