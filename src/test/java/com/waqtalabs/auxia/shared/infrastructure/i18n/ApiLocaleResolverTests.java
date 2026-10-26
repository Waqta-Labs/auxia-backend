package com.waqtalabs.auxia.shared.infrastructure.i18n;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

class ApiLocaleResolverTests {

  private final ApiLocaleResolver resolver = new ApiLocaleResolver();

  @ParameterizedTest
  @CsvSource({
    "es, es-419",
    "es-PE, es-419",
    "es-419, es-419",
    "en-US, en-US",
    "en-GB, en-US",
    "fr-FR, en-US",
    "'es-PE,en;q=0.8', es-419",
    "'fr,es;q=0.5', es-419"
  })
  void resolvesAcceptLanguageToSupportedLocale(String acceptLanguage, String expected) {
    var request = new MockHttpServletRequest();
    request.addHeader(HttpHeaders.ACCEPT_LANGUAGE, acceptLanguage);

    assertThat(resolver.resolveLocale(request)).isEqualTo(Locale.forLanguageTag(expected));
  }

  @Test
  void requestWithoutAcceptLanguageUsesEnglish() {
    assertThat(resolver.resolveLocale(new MockHttpServletRequest()))
        .isEqualTo(ApiLocaleResolver.DEFAULT_LOCALE);
  }
}
