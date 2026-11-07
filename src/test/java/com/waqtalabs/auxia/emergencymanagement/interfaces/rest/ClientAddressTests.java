package com.waqtalabs.auxia.emergencymanagement.interfaces.rest;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

class ClientAddressTests {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "190.12.4.7:52814 | 190.12.4.7",
        "190.12.4.7 | 190.12.4.7",
        "1.1.1.1, 190.12.4.7:52814 | 190.12.4.7",
        "[2001:db8::1]:52814 | 2001:db8::1",
        "2001:db8::1 | 2001:db8::1"
      })
  void lastAddressOfXForwardedForWithoutItsPort(String forwardedFor, String address) {
    var request = new MockHttpServletRequest();
    request.addHeader("X-Forwarded-For", forwardedFor);

    assertThat(ClientAddress.of(request)).isEqualTo(address);
  }

  @ParameterizedTest
  @CsvSource(value = {"''", "' '"})
  void withoutXForwardedForTheAddressOfTheConnection(String forwardedFor) {
    var request = new MockHttpServletRequest();
    request.setRemoteAddr("10.0.0.4");
    if (!forwardedFor.isEmpty()) {
      request.addHeader("X-Forwarded-For", forwardedFor);
    }

    assertThat(ClientAddress.of(request)).isEqualTo("10.0.0.4");
  }
}
