package com.waqtalabs.auxia.identityaccess.infrastructure.persistence.jpa;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.AccountStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** The JPA adapters store and rebuild organizations and user accounts. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class JpaIdentityRepositoriesTests {

  private static final PasswordHash HASH =
      new PasswordHash("$2a$10$abcdefghijklmnopqrstuuMRf3dqjSPnVA4IqU3S7wSu5tO0Pq.Ri", "BCRYPT");

  @Autowired private OrganizationRepository organizations;
  @Autowired private UserAccountRepository users;
  @Autowired private EntityManager entityManager;

  @Test
  void organizationIsStoredAndFoundByItsRuc() {
    Organization organization =
        Organization.register("COER Lima", OrganizationType.GOVERNMENT, new TaxId("20131370998"));
    organization.suspend();
    organizations.save(organization);
    clear();

    Organization found = organizations.findById(organization.id()).orElseThrow();
    assertThat(found.name()).isEqualTo("COER Lima");
    assertThat(found.type()).isEqualTo(OrganizationType.GOVERNMENT);
    assertThat(found.taxId()).isEqualTo(new TaxId("20131370998"));
    assertThat(found.status()).isEqualTo(OrganizationStatus.SUSPENDED);
    assertThat(organizations.existsByTaxId(new TaxId("20131370998"))).isTrue();
    assertThat(organizations.existsByTaxId(new TaxId("20999999999"))).isFalse();
  }

  @Test
  void userAccountIsStoredWithItsLockAndFoundByEmail() {
    Organization organization =
        organizations.save(
            Organization.register(
                "COER Lima", OrganizationType.GOVERNMENT, new TaxId("20131370998")));
    UserAccount account =
        UserAccount.register(
            organization.id(),
            new EmailAddress("rosa.quispe@coer.gob.pe"),
            new PersonName("Rosa", "Quispe"),
            Role.AUTHORITY,
            HASH);
    account =
        UserAccount.reconstitute(
            account.id(),
            account.organizationId(),
            account.email(),
            account.name(),
            account.passwordHash(),
            Role.AUTHORITY,
            AccountStatus.LOCKED,
            5,
            LocalDateTime.of(2026, 3, 14, 9, 30));
    users.save(account);
    clear();

    UserAccount found =
        users.findByEmail(new EmailAddress("ROSA.QUISPE@coer.gob.pe")).orElseThrow();
    assertThat(found.id()).isEqualTo(account.id());
    assertThat(found.organizationId()).isEqualTo(organization.id());
    assertThat(found.name()).isEqualTo(new PersonName("Rosa", "Quispe"));
    assertThat(found.passwordHash()).isEqualTo(HASH);
    assertThat(found.status()).isEqualTo(AccountStatus.LOCKED);
    assertThat(found.failedLoginAttempts()).isEqualTo(5);
    assertThat(found.lastLoginAt()).contains(LocalDateTime.of(2026, 3, 14, 9, 30));
    assertThat(users.existsByEmail(new EmailAddress("rosa.quispe@coer.gob.pe"))).isTrue();
    assertThat(users.findByOrganizationId(organization.id()))
        .extracting(UserAccount::id)
        .containsExactly(account.id());
  }

  private void clear() {
    entityManager.flush();
    entityManager.clear();
  }
}
