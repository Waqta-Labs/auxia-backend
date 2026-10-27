package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.ChangeAccountStatusCommand;
import com.waqtalabs.auxia.identityaccess.domain.exceptions.InvalidAccountStatusException;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Disables an account, or enables and unlocks it. */
@Service
public class ChangeAccountStatusCommandHandler {

  private final UserAccountRepository userAccountRepository;

  public ChangeAccountStatusCommandHandler(UserAccountRepository userAccountRepository) {
    this.userAccountRepository = userAccountRepository;
  }

  @Transactional
  public UserAccount handle(ChangeAccountStatusCommand command) {
    UserAccount account =
        ManagedUsers.load(
            userAccountRepository,
            command.administrator(),
            command.organizationId(),
            command.userId());
    switch (command.status()) {
      case ACTIVE -> account.unlock();
      case DISABLED -> account.disable();
      case LOCKED -> throw new InvalidAccountStatusException(command.status());
    }
    return userAccountRepository.save(account);
  }
}
