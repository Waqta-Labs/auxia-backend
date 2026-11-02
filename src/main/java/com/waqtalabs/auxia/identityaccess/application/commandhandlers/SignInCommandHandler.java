package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commands.SignInCommand;
import com.waqtalabs.auxia.identityaccess.application.ports.TokenIssuer;
import com.waqtalabs.auxia.identityaccess.application.ports.TokenIssuer.IssuedToken;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import com.waqtalabs.auxia.shared.application.exceptions.AuthenticationFailedException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Signs a user in and issues the access token (US-21).
 *
 * <p>Every failure, whether the email is unknown, the password is wrong, the account is locked or
 * disabled or the organization is suspended, ends in the same AuthenticationFailedException, so
 * the answer never reveals which part was wrong. A wrong password is still counted: the
 * transaction is committed even when the sign-in fails, so the fifth attempt locks the account.
 */
@Service
public class SignInCommandHandler {

  private final UserAccountRepository userAccountRepository;
  private final OrganizationRepository organizationRepository;
  private final PasswordHashingService passwordHashing;
  private final TokenIssuer tokenIssuer;
  private final Clock clock;

  public SignInCommandHandler(
      UserAccountRepository userAccountRepository,
      OrganizationRepository organizationRepository,
      PasswordHashingService passwordHashing,
      TokenIssuer tokenIssuer,
      Clock clock) {
    this.userAccountRepository = userAccountRepository;
    this.organizationRepository = organizationRepository;
    this.passwordHashing = passwordHashing;
    this.tokenIssuer = tokenIssuer;
    this.clock = clock;
  }

  @Transactional(noRollbackFor = AuthenticationFailedException.class)
  public SignedIn handle(SignInCommand command) {
    UserAccount account =
        email(command.email())
            .flatMap(userAccountRepository::findByEmail)
            .orElseThrow(() -> new AuthenticationFailedException("Unknown email"));
    boolean organizationActive =
        organizationRepository
            .findById(account.organizationId())
            .map(Organization::isActive)
            .orElse(false);
    if (!organizationActive) {
      throw new AuthenticationFailedException("Organization not active");
    }
    boolean authenticated =
        account.authenticate(command.password(), passwordHashing, LocalDateTime.now(clock));
    UserAccount saved = userAccountRepository.save(account);
    if (!authenticated) {
      throw new AuthenticationFailedException("Wrong password or account not active");
    }
    return new SignedIn(saved, tokenIssuer.issue(saved));
  }

  private static Optional<EmailAddress> email(String value) {
    try {
      return Optional.of(new EmailAddress(value));
    } catch (IllegalArgumentException | NullPointerException ex) {
      return Optional.empty();
    }
  }

  /** The signed-in user and its access token. */
  public record SignedIn(UserAccount account, IssuedToken token) {}
}
