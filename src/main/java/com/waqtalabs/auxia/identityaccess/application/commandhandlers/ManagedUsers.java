package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.domain.exceptions.SelfManagementException;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.UserAccountNotFoundException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;

/** Rules shared by the handlers in which an administrator manages another user. */
final class ManagedUsers {

  private ManagedUsers() {}

  /**
   * The user an administrator manages. A user of another organization is answered as not found,
   * so an administrator cannot learn which users other organizations have.
   *
   * @throws SelfManagementException if the administrator tries to manage their own account
   */
  static UserAccount load(
      UserAccountRepository repository,
      UserId administrator,
      OrganizationId organizationId,
      UserId userId) {
    if (administrator.equals(userId)) {
      throw new SelfManagementException(userId);
    }
    return repository
        .findById(userId)
        .filter(account -> account.belongsTo(organizationId))
        .orElseThrow(() -> new UserAccountNotFoundException(userId));
  }
}
