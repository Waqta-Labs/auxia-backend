package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import com.waqtalabs.auxia.identityaccess.application.ports.TokenIssuer;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import io.jsonwebtoken.Jwts;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import org.springframework.stereotype.Component;

/**
 * Signs access tokens with JJWT. The token carries the user ({@code sub}), its organization
 * ({@code org}) and its role ({@code role}), and names its key in the {@code kid} header.
 */
@Component
class JwtTokenIssuer implements TokenIssuer {

  static final String ORGANIZATION_CLAIM = "org";
  static final String ROLE_CLAIM = "role";

  private final JwtSigningKeyProvider keys;
  private final JwtProperties properties;
  private final Clock clock;

  JwtTokenIssuer(JwtSigningKeyProvider keys, JwtProperties properties, Clock clock) {
    this.keys = keys;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public IssuedToken issue(UserAccount account) {
    Instant issuedAt = clock.instant();
    Instant expiresAt = issuedAt.plus(properties.expiration());
    String token =
        Jwts.builder()
            .header()
            .keyId(keys.activeKeyId())
            .and()
            .issuer(properties.issuer())
            .subject(account.id().value().toString())
            .claim(ORGANIZATION_CLAIM, account.organizationId().value().toString())
            .claim(ROLE_CLAIM, account.role().name())
            .issuedAt(Date.from(issuedAt))
            .expiration(Date.from(expiresAt))
            .signWith(keys.activeKey())
            .compact();
    return new IssuedToken(token, expiresAt);
  }
}
