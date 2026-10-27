package com.waqtalabs.auxia.identityaccess.interfaces.rest.assemblers;

import com.waqtalabs.auxia.identityaccess.application.commandhandlers.SignInCommandHandler.SignedIn;
import com.waqtalabs.auxia.identityaccess.application.commands.RegisterOrganizationCommand;
import com.waqtalabs.auxia.identityaccess.application.commands.RegisterUserCommand;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.AuthenticatedUserResource;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.OrganizationResource;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.RegisterOrganizationRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.RegisterUserRequest;
import com.waqtalabs.auxia.identityaccess.interfaces.rest.resources.UserAccountResource;

public final class IdentityResourceAssembler {

  private IdentityResourceAssembler() {}

  public static RegisterOrganizationCommand toCommand(RegisterOrganizationRequest request) {
    var administrator = request.administrator();
    return new RegisterOrganizationCommand(
        request.name(),
        request.type(),
        request.taxId(),
        administrator.email(),
        administrator.firstName(),
        administrator.lastName(),
        administrator.password());
  }

  public static RegisterUserCommand toCommand(
      OrganizationId organizationId, RegisterUserRequest request) {
    return new RegisterUserCommand(
        organizationId,
        request.email(),
        request.firstName(),
        request.lastName(),
        request.role(),
        request.password());
  }

  public static AuthenticatedUserResource toResource(SignedIn signedIn) {
    UserAccount account = signedIn.account();
    return new AuthenticatedUserResource(
        signedIn.token().token(),
        "Bearer",
        signedIn.token().expiresAt(),
        account.id().value(),
        account.organizationId().value(),
        account.role(),
        account.name().fullName());
  }

  public static OrganizationResource toResource(Organization organization) {
    return new OrganizationResource(
        organization.id().value(),
        organization.name(),
        organization.type(),
        organization.taxId().value(),
        organization.status());
  }

  public static UserAccountResource toResource(UserAccount account) {
    return new UserAccountResource(
        account.id().value(),
        account.organizationId().value(),
        account.email().value(),
        account.name().firstName(),
        account.name().lastName(),
        account.role(),
        account.status(),
        account.lastLoginAt().orElse(null));
  }
}
