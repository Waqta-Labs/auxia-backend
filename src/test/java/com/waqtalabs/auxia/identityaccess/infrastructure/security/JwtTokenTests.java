package com.waqtalabs.auxia.identityaccess.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.identityaccess.application.ports.TokenIssuer.IssuedToken;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.shared.infrastructure.security.AuthenticatedUser;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.WeakKeyException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JwtTokenTests {

  private static final Instant NOW = Instant.parse("2026-03-14T14:30:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
  private static final String KEY_1 = secret("first-secret-of-at-least-32-bytes");
  private static final String KEY_2 = secret("second-secret-of-at-least-32-byte");

  private final UserAccount account =
      UserAccount.register(
          OrganizationId.generate(),
          new EmailAddress("rosa.quispe@coer.gob.pe"),
          new PersonName("Rosa", "Quispe"),
          Role.AUTHORITY,
          new PasswordHash("hash", "BCRYPT"));

  @Test
  void tokenCarriesTheUserItsOrganizationAndItsRole() {
    JwtProperties properties = properties(Map.of("k1", KEY_1), "k1");

    IssuedToken issued = issuer(properties, CLOCK).issue(account);
    AuthenticatedUser user = verifier(properties, CLOCK).verify(issued.token());

    assertThat(issued.expiresAt()).isEqualTo(NOW.plus(Duration.ofHours(8)));
    assertThat(user)
        .isEqualTo(
            new AuthenticatedUser(
                account.id().value(), account.organizationId().value(), "AUTHORITY"));
  }

  @Test
  void expiredTokenIsRejected() {
    JwtProperties properties = properties(Map.of("k1", KEY_1), "k1");
    String token = issuer(properties, CLOCK).issue(account).token();
    Clock later = Clock.offset(CLOCK, Duration.ofHours(8).plusSeconds(1));

    assertThatThrownBy(() -> verifier(properties, later).verify(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void alteredTokenIsRejected() {
    JwtProperties properties = properties(Map.of("k1", KEY_1), "k1");
    String token = issuer(properties, CLOCK).issue(account).token();
    String altered = token.substring(0, token.length() - 2) + "xy";

    assertThatThrownBy(() -> verifier(properties, CLOCK).verify(altered))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void tokenSignedWithAnUnknownKeyIsRejected() {
    String token = issuer(properties(Map.of("k2", KEY_2), "k2"), CLOCK).issue(account).token();

    assertThatThrownBy(
            () -> verifier(properties(Map.of("k1", KEY_1), "k1"), CLOCK).verify(token))
        .isInstanceOf(JwtException.class);
  }

  @Test
  void rotatedKeyKeepsTheTokensOfThePreviousOneValid() {
    String oldToken = issuer(properties(Map.of("k1", KEY_1), "k1"), CLOCK).issue(account).token();
    JwtProperties rotated = properties(Map.of("k1", KEY_1, "k2", KEY_2), "k2");

    String newToken = issuer(rotated, CLOCK).issue(account).token();

    assertThat(verifier(rotated, CLOCK).verify(oldToken).userId()).isEqualTo(account.id().value());
    assertThat(verifier(rotated, CLOCK).verify(newToken).userId()).isEqualTo(account.id().value());
  }

  @Test
  void activeKeyMustBeConfiguredAndLongEnough() {
    assertThatThrownBy(() -> new JwtSigningKeyProvider(properties(Map.of("k1", KEY_1), "k9")))
        .isInstanceOf(IllegalStateException.class);
    assertThatThrownBy(
            () -> new JwtSigningKeyProvider(properties(Map.of("k1", secret("short")), "k1")))
        .isInstanceOf(WeakKeyException.class);
  }

  @Test
  void bcryptHashesAndMatchesPasswords() {
    var hashing = new BCryptPasswordHashingService();

    PasswordHash hash = hashing.hash("Chosica-2026");

    assertThat(hash.algorithm()).isEqualTo("BCRYPT");
    assertThat(hash.value()).startsWith("$2a$").doesNotContain("Chosica-2026");
    assertThat(hashing.matches("Chosica-2026", hash)).isTrue();
    assertThat(hashing.matches("chosica-2026", hash)).isFalse();
  }

  private static JwtTokenIssuer issuer(JwtProperties properties, Clock clock) {
    return new JwtTokenIssuer(new JwtSigningKeyProvider(properties), properties, clock);
  }

  private static JwtTokenVerifier verifier(JwtProperties properties, Clock clock) {
    return new JwtTokenVerifier(new JwtSigningKeyProvider(properties), properties, clock);
  }

  private static JwtProperties properties(Map<String, String> keys, String activeKeyId) {
    return new JwtProperties(keys, activeKeyId, "auxia-backend", Duration.ofHours(8));
  }

  private static String secret(String value) {
    return Base64.getEncoder().encodeToString(value.getBytes());
  }
}
