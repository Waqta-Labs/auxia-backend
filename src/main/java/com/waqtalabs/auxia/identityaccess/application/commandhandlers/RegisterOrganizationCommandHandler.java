package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.RegisterOrganizationCommand;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.DuplicateEmailException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.DuplicateTaxIdException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registers an organization with its first administrator in one transaction. It is the only
 * handler that creates two aggregates together, because an organization without an administrator
 * could not be managed.
 */
@Service
public class RegisterOrganizationCommandHandler {

  private final OrganizationRepository organizationRepository;
  private final UserAccountRepository userAccountRepository;
  private final PasswordHashingService passwordHashing;

  public RegisterOrganizationCommandHandler(
      OrganizationRepository organizationRepository,
      UserAccountRepository userAccountRepository,
      PasswordHashingService passwordHashing) {
    this.organizationRepository = organizationRepository;
    this.userAccountRepository = userAccountRepository;
    this.passwordHashing = passwordHashing;
  }

  @Transactional
  public RegisteredOrganization handle(RegisterOrganizationCommand command) {
    var taxId = new TaxId(command.taxId());
    if (organizationRepository.existsByTaxId(taxId)) {
      throw new DuplicateTaxIdException(taxId);
    }
    var email = new EmailAddress(command.adminEmail());
    if (userAccountRepository.existsByEmail(email)) {
      throw new DuplicateEmailException(email);
    }
    Organization organization =
        organizationRepository.save(
            Organization.register(command.name(), command.type(), taxId));
    UserAccount administrator =
        userAccountRepository.save(
            UserAccount.register(
                organization.id(),
                email,
                new PersonName(command.adminFirstName(), command.adminLastName()),
                Role.ORGANIZATION_ADMIN,
                passwordHashing.hash(command.adminPassword())));
    return new RegisteredOrganization(organization, administrator);
  }

  public record RegisteredOrganization(Organization organization, UserAccount administrator) {}
}
