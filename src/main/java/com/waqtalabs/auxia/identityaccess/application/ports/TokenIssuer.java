package com.waqtalabs.auxia.identityaccess.application.ports;

import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import java.time.Instant;

/** Issues the access token of a user who signed in. */
public interface TokenIssuer {

  IssuedToken issue(UserAccount account);

  /** A signed token and the moment it stops being valid. */
  record IssuedToken(String token, Instant expiresAt) {}
}
