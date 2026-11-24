package com.waqtalabs.auxia.identityaccess.application.commandhandlers;

import com.waqtalabs.auxia.identityaccess.application.commandhandlers.SignInCommandHandler.SignedIn;
import com.waqtalabs.auxia.identityaccess.application.commands.SignInCommand;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.repositories.BrigadeRegistrationRepository;
import com.waqtalabs.auxia.shared.application.exceptions.AuthenticationFailedException;
import org.springframework.stereotype.Service;

/**
 * Signs in a brigadist whose request was approved, with the secret of the phone instead of an
 * e-mail and a password (A_02). It is the same sign-in of the account: same token, same count of
 * failed attempts and same lock.
 */
@Service
public class DeviceSignInCommandHandler {

  private final BrigadeRegistrationRepository registrationRepository;
  private final SignInCommandHandler signIn;

  public DeviceSignInCommandHandler(
      BrigadeRegistrationRepository registrationRepository, SignInCommandHandler signIn) {
    this.registrationRepository = registrationRepository;
    this.signIn = signIn;
  }

  /**
   * @throws AuthenticationFailedException if the request is unknown or not approved, or the
   *     secret is wrong, always with the same answer
   */
  public SignedIn handle(BrigadeRegistrationId registrationId, String deviceSecret) {
    var registration =
        registrationRepository
            .findById(registrationId)
            .filter(found -> found.status() == RegistrationStatus.APPROVED)
            .orElseThrow(() -> new AuthenticationFailedException("Registration not approved"));
    return signIn.handle(new SignInCommand(registration.accountEmail().value(), deviceSecret));
  }
}
