package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.RegisterUserCommand;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.DuplicateEmailException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.OrganizationNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.OrganizationSuspendedException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers a user in an active organization, with an email no other user has. */
@Service
public class RegisterUserCommandHandler {

  private final OrganizationRepository organizationRepository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordHashingService passwordHashing;

  public RegisterUserCommandHandler(
      OrganizationRepository organizationRepository,
      UserAccountRepository userAccountRepository,
      PasswordHashingService passwordHashing) {
    this.organizationRepository = organizationRepository;
    this.userAccountRepository = userAccountRepository;
    this.passwordHashing = passwordHashing;
  }

  @Transactional
  public UserAccount handle(RegisterUserCommand command) {
    Organization organization =
        organizationRepository
            .findById(command.organizationId())
            .orElseThrow(() -> new OrganizationNotFoundException(command.organizationId()));
    if (!organization.isActive()) {
      throw new OrganizationSuspendedException(organization.id());
    }
    var email = new EmailAddress(command.email());
    if (userAccountRepository.existsByEmail(email)) {
      throw new DuplicateEmailException(email);
    }
    return userAccountRepository.save(
        UserAccount.register(
            organization.id(),
            email,
            new PersonName(command.firstName(), command.lastName()),
            command.role(),
            passwordHashing.hash(command.password())));
  }
}
