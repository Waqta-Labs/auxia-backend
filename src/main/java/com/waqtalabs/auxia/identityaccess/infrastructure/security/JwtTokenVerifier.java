package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.ProtectedHeader;
import java.security.Key;
import java.time.Clock;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Checks the signature, issuer and expiration of a token and reads its user. */
@Component
class JwtTokenVerifier {

  private final JwtParser parser;

  JwtTokenVerifier(JwtSigningKeyProvider keys, JwtProperties properties, Clock clock) {
    this.parser =
        Jwts.parser()
            .keyLocator(
                new LocatorAdapter<Key>() {
                  @Override
                  protected Key locate(ProtectedHeader header) {
                    return keys.keyFor(header.getKeyId())
                        .orElseThrow(() -> new JwtException("Unknown signing key"));
                  }
                })
            .requireIssuer(properties.issuer())
            .clock(() -> Date.from(clock.instant()))
            .build();
  }

  /**
   * The user of a valid token.
   *
   * @throws JwtException if the token is malformed, expired or signed with an unknown key
   */
  AuthenticatedUser verify(String token) {
    Claims claims = parser.parseSignedClaims(token).getPayload();
    try {
      return new AuthenticatedUser(
          UUID.fromString(claims.getSubject()),
          UUID.fromString(claims.get(JwtTokenIssuer.ORGANIZATION_CLAIM, String.class)),
          claims.get(JwtTokenIssuer.ROLE_CLAIM, String.class));
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw new JwtException("Token without a valid user", ex);
    }
  }
}
