package com.waqtalabs.auxia.identityaccess.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.EmailAddress;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationStatus;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.OrganizationType;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.PersonName;
import com.waqtalabs.auxia.identityaccess.domain.model.valueobjects.TaxId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OrganizationTests {

  @Test
  void organizationStartsActiveAndCanBeSuspendedAndReactivated() {
    Organization organization =
        Organization.register(" COER Lima ", OrganizationType.GOVERNMENT, new TaxId("20131370998"));

    assertThat(organization.name()).isEqualTo("COER Lima");
    assertThat(organization.isActive()).isTrue();

    organization.suspend();
    assertThat(organization.status()).isEqualTo(OrganizationStatus.SUSPENDED);
    assertThat(organization.isActive()).isFalse();

    organization.reactivate();
    assertThat(organization.isActive()).isTrue();
  }

  @ParameterizedTest
  @ValueSource(strings = {"2013137099", "201313709981", "2013137099A"})
  void rucHasElevenDigits(String value) {
    assertThatThrownBy(() -> new TaxId(value)).isInstanceOf(IllegalArgumentException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"rosa", "rosa@coer", "rosa quispe@coer.gob.pe", "@coer.gob.pe"})
  void invalidEmailIsRejected(String value) {
    assertThatThrownBy(() -> new EmailAddress(value)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void personNameJoinsFirstAndLastNames() {
    assertThat(new PersonName(" Rosa ", "Quispe Huamán").fullName())
        .isEqualTo("Rosa Quispe Huamán");
  }
}
