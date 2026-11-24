package com.waqtalabs.auxia.identityaccess.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.identityaccess.domain.exceptions.RegistrationAlreadyDecidedException;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.BrigadeRegistrationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationId;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PasswordHash;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.RegistrationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.UserId;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BrigadeRegistrationTests {

  private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 7, 10, 0);

  private final UserId admin = new UserId(UUID.randomUUID());
  private final BrigadeRegistration registration = submit("  Miguel Ángel Quispe ");

  @Test
  void requestWaitsForTheOrganizationWithAnInternalAccountEmail() {
    assertThat(registration.status()).isEqualTo(RegistrationStatus.PENDING);
    assertThat(registration.fullName()).isEqualTo("Miguel Ángel Quispe");
    assertThat(registration.accountName()).isEqualTo(new PersonName("Miguel", "Ángel Quispe"));
    assertThat(registration.accountEmail().value())
        .isEqualTo("brigadist-" + registration.id().value() + "@device.auxia.invalid");
    assertThat(submit("Miguel").accountName()).isEqualTo(new PersonName("Miguel", "Miguel"));
  }

  @Test
  void approvalKeepsTheAccountAndWhoDecided() {
    UserId account = new UserId(UUID.randomUUID());

    registration.approve(account, admin, NOW);

    assertThat(registration.status()).isEqualTo(RegistrationStatus.APPROVED);
    assertThat(registration.userId()).contains(account);
    assertThat(registration.decidedBy()).contains(admin);
    assertThat(registration.decidedAt()).contains(NOW);
  }

  @Test
  void requestIsDecidedOnceAndARejectionSaysWhy() {
    registration.reject("  Código de otro organismo ", admin, NOW);

    assertThat(registration.status()).isEqualTo(RegistrationStatus.REJECTED);
    assertThat(registration.rejectionReason()).contains("Código de otro organismo");
    assertThatThrownBy(() -> registration.approve(new UserId(UUID.randomUUID()), admin, NOW))
        .isInstanceOf(RegistrationAlreadyDecidedException.class);
    assertThatThrownBy(() -> submit("Rosa").reject(" ", admin, NOW))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private static BrigadeRegistration submit(String fullName) {
    return BrigadeRegistration.submit(
        new BrigadeRegistrationId(UUID.randomUUID()),
        fullName,
        "987654321",
        "Defensa Civil",
        "BRG-ALFA-07",
        UUID.randomUUID(),
        new OrganizationId(UUID.randomUUID()),
        new PasswordHash("hash", "BCRYPT"),
        null,
        NOW);
  }
}
