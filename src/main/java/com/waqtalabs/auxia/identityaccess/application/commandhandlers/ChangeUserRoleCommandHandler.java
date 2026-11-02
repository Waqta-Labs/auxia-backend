package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.ChangeUserRoleCommand;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Changes the role of a user (US-22). The new role applies from the next sign-in, and at once in
 * the role checks of critical commands, which read the account every time.
 */
@Service
public class ChangeUserRoleCommandHandler {

  private final UserAccountRepository userAccountRepository;

  public ChangeUserRoleCommandHandler(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional
  public UserAccount handle(ChangeUserRoleCommand command) {
    UserAccount account =
        ManagedUsers.load(
            userAccountRepository,
            command.administrator(),
            command.organizationId(),
            command.userId());
    account.changeRole(command.role());
    return userAccountRepository.save(account);
  }
}
