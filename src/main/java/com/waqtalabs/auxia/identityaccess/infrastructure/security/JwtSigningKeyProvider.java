package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * HMAC keys of the tokens, each one identified by a {@code kid}. The active key signs; any known
 * key verifies, so a key can be rotated without invalidating the tokens already issued.
 */
@Component
class JwtSigningKeyProvider {

  private final Map<String, SecretKey> keys;
  private final String activeKeyId;

  JwtSigningKeyProvider(JwtProperties properties) {
    // Keys.hmacShaKeyFor rejects secrets shorter than 256 bits.
    this.keys =
        properties.keys().entrySet().stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    entry -> Keys.hmacShaKeyFor(Decoders.BASE64.decode(entry.getValue()))));
    if (!keys.containsKey(properties.activeKeyId())) {
      throw new IllegalStateException("The active JWT key is not among the configured keys");
    }
    this.activeKeyId = properties.activeKeyId();
  }

  String activeKeyId() {
    return activeKeyId;
  }

  SecretKey activeKey() {
    return keys.get(activeKeyId);
  }

  Optional<SecretKey> keyFor(String keyId) {
    return Optional.ofNullable(keyId).map(keys::get);
  }
}
