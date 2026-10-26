package com.waqtalabs.auxia.shared.infrastructure.i18n;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Locale;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Resolves the language of each request from its {@code Accept-Language} header.
 *
 * <p>The API answers in English ({@code en_US}, the default) and in Latin American Spanish
 * ({@code es_419}). Any Spanish variant, such as {@code es-PE}, is answered in {@code es_419};
 * any other language falls back to English.
 */
public class ApiLocaleResolver extends AcceptHeaderLocaleResolver {

  public static final Locale DEFAULT_LOCALE = Locale.US;
  public static final Locale LATIN_AMERICAN_SPANISH = Locale.forLanguageTag("es-419");

  private static final Locale SPANISH = Locale.of("es");

  public ApiLocaleResolver() {
    setSupportedLocales(List.of(DEFAULT_LOCALE, SPANISH));
    setDefaultLocale(DEFAULT_LOCALE);
  }

  @Override
  public Locale resolveLocale(HttpServletRequest request) {
    Locale locale = super.resolveLocale(request);
    return SPANISH.getLanguage().equals(locale.getLanguage())
        ? LATIN_AMERICAN_SPANISH
        : DEFAULT_LOCALE;
  }
}
