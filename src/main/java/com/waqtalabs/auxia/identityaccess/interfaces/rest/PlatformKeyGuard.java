package com.waqtalabs.auxia.identityaccess.interfaces.rest;

import com.waqtalabs.auxia.shared.application.exceptions.AuthenticationFailedException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Checks the key of the platform operator, who registers and suspends organizations. AuxIA has no
 * platform administrator role: the operator proves who they are with this secret.
 */
@Component
class PlatformKeyGuard {

  static final String HEADER = "X-Platform-Key";

  private final byte[] platformKey;

  PlatformKeyGuard(@Value("${auxia.security.platform-admin-key}") String platformKey) {
    this.platformKey = platformKey.getBytes(StandardCharsets.UTF_8);
  }

  /**
   * Accepts the request only with the right key, compared in constant time.
   *
   * @throws AuthenticationFailedException if the key is missing or wrong
   */
  void check(String key) {
    byte[] received = key == null ? new byte[0] : key.getBytes(StandardCharsets.UTF_8);
    if (!MessageDigest.isEqual(platformKey, received)) {
      throw new AuthenticationFailedException("Missing or wrong platform key");
    }
  }
}
