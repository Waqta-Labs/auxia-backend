package com.waqtalabs.auxia.identityaccess.interfaces.acl;

import static org.assertj.core.api.Assertions.assertThat;

import com.waqtalabs.auxia.TestcontainersConfiguration;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.Organization;
import com.waqtalabs.auxia.identityaccess.domain.model.aggregates.UserAccount;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.Role;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import com.waqtalabs.auxia.identityaccess.domain.repositories.OrganizationRepository;
import com.waqtalabs.auxia.identityaccess.domain.repositories.UserAccountRepository;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

/** Other bounded contexts check the current role and status of a user through the facade. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class IdentityAccessContextFacadeTests {

  private static final PasswordHash HASH =
      new PasswordHash("$2a$10$abcdefghijklmnopqrstuuMRf3dqjSPnVA4IqU3S7wSu5tO0Pq.Ri", "BCRYPT");

  @Autowired private IdentityAccessContextFacade facade;
  @Autowired private OrganizationRepository organizations;
  @Autowired private UserAccountRepository users;

  @Test
  void activeUserHasOnlyTheirCurrentRole() {
    Organization organization = organization();
    UserAccount authority = user(organization, Role.AUTHORITY);
    UUID userId = authority.id().value();

    assertThat(facade.isActiveUserWithRole(userId, "FIELD_BRIGADE", "AUTHORITY")).isTrue();
    assertThat(facade.isActiveUserWithRole(userId, "AUDITOR")).isFalse();
    assertThat(facade.isActiveUserWithRole(userId, "ROOT")).isFalse();
    assertThat(facade.isActiveUserWithRole(userId)).isFalse();

    authority.changeRole(Role.AUDITOR);
    users.save(authority);
    assertThat(facade.isActiveUserWithRole(userId, "AUTHORITY")).isFalse();
    assertThat(facade.isActiveUserWithRole(userId, "AUDITOR")).isTrue();
  }

  @Test
  void disabledUserOrSuspendedOrganizationHasNoAccess() {
    Organization organization = organization();
    UserAccount disabled = user(organization, Role.AUTHORITY);
    disabled.disable();
    users.save(disabled);
    UserAccount active = user(organization, Role.AUTHORITY);

    assertThat(facade.isActiveUserWithRole(disabled.id().value(), "AUTHORITY")).isFalse();
    assertThat(facade.isActiveUserWithRole(active.id().value(), "AUTHORITY")).isTrue();

    organization.suspend();
    organizations.save(organization);
    assertThat(facade.isActiveUserWithRole(active.id().value(), "AUTHORITY")).isFalse();
  }

  @Test
  void organizationOfAUserIsFoundByItsId() {
    Organization organization = organization();
    UserAccount user = user(organization, Role.FIELD_BRIGADE);

    assertThat(facade.getOrganizationId(user.id().value())).contains(organization.id().value());
    assertThat(facade.getOrganizationId(UUID.randomUUID())).isEmpty();
    assertThat(facade.isActiveUserWithRole(UUID.randomUUID(), "AUTHORITY")).isFalse();
  }

  private Organization organization() {
    String taxId = "20" + ThreadLocalRandom.current().nextLong(100_000_000L, 1_000_000_000L);
    return organizations.save(
        Organization.register("COER Lima", OrganizationType.GOVERNMENT, new TaxId(taxId)));
  }

  private UserAccount user(Organization organization, Role role) {
    return users.save(
        UserAccount.register(
            organization.id(),
            new EmailAddress("user-" + UUID.randomUUID() + "@auxia.pe"),
            new PersonName("Rosa", "Quispe"),
            role,
            HASH));
  }
}
