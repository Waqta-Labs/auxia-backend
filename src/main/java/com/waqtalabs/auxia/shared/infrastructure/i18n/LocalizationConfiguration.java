package com.waqtalabs.auxia.shared.infrastructure.i18n;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;

/** Registers the locale resolver of the API; texts live in {@code messages*.properties}. */
@Configuration
public class LocalizationConfiguration {

  @Bean
  LocaleResolver localeResolver() {
    return new ApiLocaleResolver();
  }
}
