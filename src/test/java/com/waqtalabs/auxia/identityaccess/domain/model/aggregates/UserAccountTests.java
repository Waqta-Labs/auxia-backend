package com.waqtalabs.auxia.identityaccess.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.services.PasswordHashingService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UserAccountTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 14, 9, 30);
  private static final String PASSWORD = "Chosica-2026";

  /** Test double: the hash of a password is the password reversed. */
  private static final PasswordHashingService HASHING =
      new PasswordHashingService() {
        @Override
        public PasswordHash hash(String rawPassword) {
          return new PasswordHash(new StringBuilder(rawPassword).reverse().toString(), "TEST");
        }

        @Override
        public boolean matches(String rawPassword, PasswordHash hash) {
          return hash(rawPassword).value().equals(hash.value());
        }
      };

  private final OrganizationId organization = OrganizationId.generate();
  private final UserAccount account =
      UserAccount.register(
          organization,
          new EmailAddress(" Rosa.Quispe@COER.gob.pe "),
          new PersonName("Rosa", "Quispe"),
          Role.AUTHORITY,
          HASHING.hash(PASSWORD));

  @Test
  void registeredAccountIsActiveInItsOrganization() {
    assertThat(account.email().value()).isEqualTo("rosa.quispe@coer.gob.pe");
    assertThat(account.belongsTo(organization)).isTrue();
    assertThat(account.hasRole(Role.AUTHORITY)).isTrue();
    assertThat(account.isActive()).isTrue();
    assertThat(account.failedLoginAttempts()).isZero();
    assertThat(account.lastLoginAt()).isEmpty();
  }

  @Test
  void rightPasswordAuthenticatesAndRecordsTheSignIn() {
    account.authenticate("wrong", HASHING, NOW);

    assertThat(account.authenticate(PASSWORD, HASHING, NOW)).isTrue();
    assertThat(account.failedLoginAttempts()).isZero();
    assertThat(account.lastLoginAt()).contains(NOW);
  }

  @Test
  void fifthFailedSignInLocksTheAccount() {
    for (int attempt = 1; attempt < UserAccount.MAX_FAILED_ATTEMPTS; attempt++) {
      assertThat(account.authenticate("wrong", HASHING, NOW)).isFalse();
    }
    assertThat(account.isActive()).isTrue();

    assertThat(account.authenticate("wrong", HASHING, NOW)).isFalse();

    assertThat(account.status()).isEqualTo(AccountStatus.LOCKED);
    assertThat(account.failedLoginAttempts()).isEqualTo(5);
    assertThat(account.authenticate(PASSWORD, HASHING, NOW)).isFalse();
  }

  @Test
  void unlockingRestoresTheAccount() {
    for (int attempt = 0; attempt < UserAccount.MAX_FAILED_ATTEMPTS; attempt++) {
      account.authenticate("wrong", HASHING, NOW);
    }

    account.unlock();

    assertThat(account.isActive()).isTrue();
    assertThat(account.failedLoginAttempts()).isZero();
    assertThat(account.authenticate(PASSWORD, HASHING, NOW)).isTrue();
  }

  @Test
  void disabledAccountNeverAuthenticates() {
    account.disable();

    assertThat(account.authenticate(PASSWORD, HASHING, NOW)).isFalse();
    assertThat(account.failedLoginAttempts()).isZero();
  }

  @Test
  void roleAndPasswordChange() {
    account.changeRole(Role.AUDITOR);
    account.changePassword(HASHING.hash("Nueva-clave-2026"));

    assertThat(account.hasRole(Role.AUDITOR)).isTrue();
    assertThat(account.authenticate("Nueva-clave-2026", HASHING, NOW)).isTrue();
    assertThat(account.passwordHash().toString()).doesNotContain(account.passwordHash().value());
  }
}
